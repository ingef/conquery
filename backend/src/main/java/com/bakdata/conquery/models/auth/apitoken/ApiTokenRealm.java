package com.bakdata.conquery.models.auth.apitoken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.auth.ApiTokenDataRepresentation;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.StoreMappings;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.entities.Userish;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentClosedException;
import jetbrains.exodus.env.Environments;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.CharArrayBuffer;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.util.Destroyable;


/**
 * This realm provides and checks long-living API tokens. The tokens support a limited scope of actions that is backed
 * by the actual permissions of the invoking user.
 *
 */
@Slf4j
public class ApiTokenRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm, Destroyable {

	private static final int ENVIRONMNENT_CLOSING_RETRYS = 2;
	private static final int ENVIRONMNENT_CLOSING_TIMEOUT = 2; // seconds

	private final Path storageDir;
	private final XodusConfig storeConfig;
	private final Validator validator;
	private final ObjectMapper objectMapper;
	private final ArrayList<XodusStore> openStoresInEnv = new ArrayList<>();
	private final MetaStorage storage;
	private final ApiTokenCreator apiTokenCreator = new ApiTokenCreator();

	private transient Environment tokenEnvironment;
	private transient Store<ApiTokenHash, ApiTokenData> tokenDataStore;
	private transient Store<UUID, ApiTokenData.MetaData> tokenMetaDataStore;

	public ApiTokenRealm(MetaStorage storage, Path storageDir, XodusConfig storeConfig, Validator validator, ObjectMapper objectMapper) {
		this.storage = storage;
		this.storageDir = storageDir;
		this.storeConfig = storeConfig;
		this.validator = validator;
		this.objectMapper = objectMapper;
		this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
		this.setAuthenticationTokenClass(ApiToken.class);
	}


	@Override
	protected void onInit() {
		super.onInit();
		// Open/create the database/store
		String storeName = "api-token";
		File tokenStore = new File(storageDir.toFile(), storeName);
		tokenEnvironment = Environments.newInstance(tokenStore, storeConfig.createConfig());

		final XodusStore data = new XodusStore(
				tokenEnvironment,
				"DATA",
				this::closeStoreHook,
				this::removeStoreHook
		);
		tokenDataStore = StoreMappings.cached(new SerializingStore<>(
				data,
				validator,
				objectMapper,
				ApiTokenHash.class,
				ApiTokenData.class,
				true,
				false,
				null
		));
		openStoresInEnv.add(data);

		final XodusStore meta = new XodusStore(
				tokenEnvironment,
				"META",
				this::closeStoreHook,
				this::removeStoreHook
		);
		tokenMetaDataStore = StoreMappings.cached(new SerializingStore<>(
				meta,
				validator,
				objectMapper,
				UUID.class,
				ApiTokenData.MetaData.class,
				true,
				false,
				null
		));
		openStoresInEnv.add(meta);
	}


	private void removeStoreHook(XodusStore store) {
		openStoresInEnv.remove(store);

		if (!openStoresInEnv.isEmpty()){
			return;
		}

		final Environment environment = store.getEnvironment();
		log.info("Removed last XodusStore in Environment. Removing Environment as well: {}", environment.getLocation());

		final List<String> xodusStores= environment.computeInReadonlyTransaction(environment::getAllStoreNames);

		if (!xodusStores.isEmpty()){
			throw new IllegalStateException("Cannot delete environment, because it still contains these stores:" + xodusStores);
		}

		environment.close();

		try {
			FileUtil.deleteRecursive(Path.of(environment.getLocation()));
		}
		catch (IOException e) {
			log.error("Cannot delete directory of removed Environment[{}]", environment.getLocation(), e);
		}
	}

	private void closeStoreHook(XodusStore store) {
		openStoresInEnv.remove(store);
		final Environment environment = store.getEnvironment();
		if (!openStoresInEnv.isEmpty()){
			return;
		}
		if (!environment.isOpen()) {
			return;
		}
		environment.close();
	}

	@Override
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(token instanceof ApiToken)) {
			return null;
		}

		final ApiToken apiToken = ((ApiToken) token);
		ApiTokenHash tokenHash = ApiTokenCreator.hashToken(apiToken);

		// Clear the token
		apiToken.clear();


		ApiTokenData tokenData = tokenDataStore.get(tokenHash);
		if (tokenData == null) {
			log.trace("Unknown token, cannot map token hash to token data. Aborting authentication");
			throw new IncorrectCredentialsException();
		}

		final ApiTokenData.MetaData metaData = new ApiTokenData.MetaData(LocalDate.now());
		tokenMetaDataStore.update(tokenData.getId(), metaData);

		final UserId userId = tokenData.getUserId();
		final User user = storage.getUser(userId);

		if (user == null) {
			throw new UnknownAccountException("The UserId does not map to a user: " + userId);
		}

		return new ConqueryAuthenticationInfo(new UserToken(user, tokenData), token, this, false);
	}

	public ApiToken createApiToken(User user, ApiTokenDataRepresentation.Request tokenRequest) {

		ApiToken token;

		synchronized (this) {
			ApiTokenHash hash;
			// Generate a token that does not collide with another tokens hash
			do {
				token = apiTokenCreator.createToken();
				hash = ApiTokenCreator.hashToken(token);

			} while(tokenDataStore.get(hash) != null);

			final ApiTokenData apiTokenData = toInternalRepresentation(tokenRequest, user, hash, storage);

			tokenDataStore.add(hash, apiTokenData);
		}

		return token;
	}

	public List<ApiTokenDataRepresentation.Response> listUserToken(Userish user) {
		ArrayList<ApiTokenDataRepresentation.Response> summary = new ArrayList<>();

		final Collection<ApiTokenData> allToken = tokenDataStore.getAll();
		for (ApiTokenData apiTokenData : allToken) {
			// Find all token data belonging to a user
			if (!user.getId().equals(apiTokenData.getUserId())){
				continue;
			}

			// Fill in the response with the details
			final ApiTokenDataRepresentation.Response response = new ApiTokenDataRepresentation.Response();
			response.setId(apiTokenData.getId());
			response.setCreationDate(apiTokenData.getCreationDate());
			response.setName(apiTokenData.getName());
			response.setExpirationDate(apiTokenData.getExpirationDate());
			response.setScopes(apiTokenData.getScopes());

			// If the token was ever used it should have an meta data entry
			ApiTokenData.MetaData meta = tokenMetaDataStore.get(apiTokenData.getId());
			if (meta != null) {
				response.setLastUsed(meta.getLastUsed());
			}
			summary.add(response);
		}
		return summary;
	}

	public void deleteToken(@NotNull Userish user, @NonNull UUID tokenId) {
		AtomicReference<ApiTokenHash> targetHash = new AtomicReference<>();

		// Find the corresponding token data and extract its hash
		for (ApiTokenData apiTokenData : tokenDataStore.getAll()) {
			if (tokenId.equals(apiTokenData.getId())) {
				targetHash.set(apiTokenData.getTokenHash());
				break;
			}
		}


		final ApiTokenHash hash = targetHash.get();
		if (hash == null) {
			log.warn("No token with id {} was found", tokenId);
			return;
		}

		synchronized (this) {
			// This should never return null
			ApiTokenData data = tokenDataStore.get(hash);
			if (data == null) {
				throw new IllegalStateException("Unable to retrieve token data for hash.");
			}

			user.authorize(data, Ability.DELETE);

			tokenDataStore.remove(hash);
			tokenMetaDataStore.remove(tokenId);
		}

		hash.clear();
	}


	private static ApiTokenData toInternalRepresentation(
			ApiTokenDataRepresentation.Request apiTokenRequest,
			User user,
			ApiTokenHash hash,
			MetaStorage storage) {
		return new ApiTokenData(
				UUID.randomUUID(),
				hash,
				apiTokenRequest.getName(),
				user.getId(),
				LocalDate.now(),
				apiTokenRequest.getExpirationDate(),
				apiTokenRequest.getScopes(),
				storage
		);
	}

	@Override
	public void destroy() throws InterruptedException {
		for(int retries = 0; retries < ENVIRONMNENT_CLOSING_RETRYS; retries++) {
			try {
				log.info("Closing the password environment.");
				tokenEnvironment.close();
				return;
			}
			catch (EnvironmentClosedException e) {
				log.warn("Password environment was already closed, which is odd but mayby the stop() lifecycle event fired twice");
				return;
			}
			catch (ExodusException e) {
				if (retries == 0) {
					log.info("The environment is still working on some transactions. Retry");
				}
				log.info("Waiting for {} seconds to retry.", ENVIRONMNENT_CLOSING_TIMEOUT);
				Thread.sleep(ENVIRONMNENT_CLOSING_TIMEOUT*1000 /* milliseconds */);
			}
		}
		// Close the environment with force
		log.info("Closing the environment forcefully");
		tokenEnvironment.getEnvironmentConfig().setEnvCloseForcedly(true);
		tokenEnvironment.close();

	}

}
