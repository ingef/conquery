package com.bakdata.conquery.models.auth.basic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import jakarta.validation.Validator;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.apiv1.auth.CredentialType;
import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.apiv1.auth.PasswordHashCredential;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.CachedStore;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.UserManageable;
import com.bakdata.conquery.models.auth.basic.PasswordHasher.HashEntry;
import com.bakdata.conquery.models.auth.conquerytoken.ConqueryTokenRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.google.common.collect.ImmutableList;
import com.password4j.HashingFunction;
import com.password4j.Password;
import io.dropwizard.util.Duration;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentClosedException;
import jetbrains.exodus.env.Environments;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.CredentialsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.lang.util.Destroyable;
import org.apache.shiro.realm.AuthenticatingRealm;

/**
 * This realm stores credentials in a local database ({@link XodusStore}). Upon
 * successful authentication using username and password the authenticated user
 * is given a signed JWT for further authentication over following requests. The
 * realm offers a basic user management, which is decoupled form the
 * authorization related user information that is saved in the
 * {@link MetaStorage}. So adding or removing a user in this realm does
 * not change the {@link MetaStorage}. {@link Conquery} interacts with
 * this realm using the Shiro framework. However, endusers can interface it
 * through specific endpoints that are registerd by this realm.
 */
@Slf4j
public class LocalAuthenticationRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm, UserManageable, AccessTokenCreator, Destroyable {

	private static final int ENVIRONMENT_CLOSING_RETRIES = 2;
	private static final Duration ENVIRONMENT_CLOSING_TIMEOUT = Duration.seconds(2);

	// Get the path for the storage here, so it is set as soon the first class is instantiated (in the ManagerNode)
	// In the DistributedStandaloneCommand this directory is overriden multiple times before LocalAuthenticationRealm::onInit for the ShardNodes, so this is a problem.
	private final File storageDir;
	private final XodusConfig passwordStoreConfig;
	private final String storeName;
	private final ConqueryTokenRealm centralTokenRealm;
	private final Duration validDuration;
	private final Validator validator;
	private final ObjectMapper mapper;
	private final HashingFunction defaultHashingFunction;
	private final CaffeineSpec caffeineSpec;
	private final MetricRegistry metricRegistry;

	private Environment passwordEnvironment;
	private Store<UserId, HashEntry> passwordStore;

	//////////////////// INITIALIZATION ////////////////////

	public LocalAuthenticationRealm(Validator validator, ObjectMapper mapper, ConqueryTokenRealm centralTokenRealm, String storeName, File storageDir, XodusConfig passwordStoreConfig, Duration validDuration, HashingFunction defaultHashingFunction,
									CaffeineSpec caffeineSpec, MetricRegistry metricRegistry) {
		this.validator = validator;
		this.mapper = mapper;
		this.defaultHashingFunction = defaultHashingFunction;
		this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
		this.storeName = storeName;
		this.storageDir = storageDir;
		this.centralTokenRealm = centralTokenRealm;
		this.passwordStoreConfig = passwordStoreConfig;
		this.validDuration = validDuration;
		this.caffeineSpec = caffeineSpec;
		this.metricRegistry = metricRegistry;
	}

	@Override
	protected void onInit() {
		super.onInit();
		// Open/create the database/store
		File passwordStoreFile = new File(storageDir, storeName);
		passwordEnvironment = Environments.newInstance(passwordStoreFile, passwordStoreConfig.createConfig());
		passwordStore = new CachedStore<>(
				new SerializingStore<>(
						new XodusStore(
								passwordEnvironment,
								"passwords",
								store -> store.getEnvironment().close(),
								store -> {
								}
						),
						validator,
						mapper,
						UserId.class,
						HashEntry.class,
						false,
						true,
						null, Executors.newSingleThreadExecutor()
				),
				caffeineSpec,
				metricRegistry
		);
	}

	//////////////////// AUTHENTICATION ////////////////////

	//////////////////// FOR JWT
	/**
	 *  Should not be called since the tokens are now handled by the ConqueryTokenRealm.
	 */
	@Override
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		throw new UnsupportedOperationException("Should not be called since the tokens are now handled by the ConqueryTokenRealm.");
	}

	//////////////////// FOR USERNAME/PASSWORD

	public String createAccessToken(String username, String password) {
		if (username.isEmpty()) {
			throw new IncorrectCredentialsException("Username was empty");
		}
		if (password.isEmpty()) {
			throw new IncorrectCredentialsException("Password was empty");
		}
		final UserId userId = new UserId(username);
		HashEntry hashedEntry = passwordStore.get(userId);
		if (hashedEntry == null) {
			throw new CredentialsException("No password hash was found for user: " + username);
		}

		final String hash = hashedEntry.hash();
		if (!Password.check(password.getBytes(), hash.getBytes()).with(PasswordHelper.getHashingFunction(hash))) {
			throw new IncorrectCredentialsException("Password was was invalid for user: " + userId);
		}

		return centralTokenRealm.createTokenForUser(userId, validDuration);
	}

	@Override
	public boolean addUser(@NonNull User user, @NonNull CredentialType credential) {

		try {
			final HashEntry hashEntry = toHashEntry(credential);
			passwordStore.add(user.getId(), hashEntry);
			log.debug("Added user to realm: {}", user.getId());
			return true;
		}
		catch (IllegalArgumentException e) {
			log.warn("Unable to add user '{}'", user.getId(), e);
		}
		return false;
	}

	//////////////////// USER MANAGEMENT ////////////////////

	/**
	 * Converts the provided password to a Xodus compatible hash.
	 */
	private HashEntry toHashEntry(CredentialType credential) {


		if (credential instanceof PasswordCredential passwordCredential) {
			return new HashEntry(Password.hash(passwordCredential.password())
										 .with(defaultHashingFunction)
										 .getResult());
		}
		else if (credential instanceof PasswordHashCredential passwordHashCredential) {
			return new HashEntry(passwordHashCredential.hash());
		}

		throw new IllegalArgumentException("CredentialType not supported yet: " + credential.getClass());
	}

	@Override
	public boolean updateUser(User user, CredentialType credential) {

		if (credential == null) {
			log.warn("Skipping user '{}' because no credential was provided", user.getId());
			return false;
		}

		try {
			final HashEntry hashEntry = toHashEntry(credential);
			passwordStore.update(user.getId(), hashEntry);
			return true;
		}
		catch (IllegalArgumentException e) {
			log.warn("Unable to update user '{}'", user.getId(), e);
		}
		return false;

	}

	@Override
	public boolean removeUser(User user) {
		passwordStore.remove(user.getId());
		return true;
	}

	@Override
	public List<UserId> getAllUsers() {
		return ImmutableList.copyOf(passwordStore.getAllKeys().toList());
	}


	
	//////////////////// LIFECYCLE MANAGEMENT ////////////////////
		
	@Override
	@SneakyThrows(IOException.class)
	public void destroy() throws InterruptedException {
		for(int retries = 0; retries < ENVIRONMENT_CLOSING_RETRIES; retries++) {
			try {
				log.info("Closing the password environment.");
				passwordStore.close();
				return;
			}
			catch (EnvironmentClosedException e) {
				log.warn("Password environment was already closed, which is odd but maybe the stop() lifecycle event fired twice");
				return;
			}
			catch (ExodusException e) {
				if (retries == 0) {
					log.info("The environment is still working on some transactions. Retry");				
				}
				log.info("Waiting for {} seconds to retry.", ENVIRONMENT_CLOSING_TIMEOUT);

				Thread.sleep(ENVIRONMENT_CLOSING_TIMEOUT.toJavaDuration());
			}
		}
		// Close the environment with force
		log.info("Closing the environment forcefully");
		passwordEnvironment.getEnvironmentConfig().setEnvCloseForcedly(true);
		passwordEnvironment.close();

	}
}
