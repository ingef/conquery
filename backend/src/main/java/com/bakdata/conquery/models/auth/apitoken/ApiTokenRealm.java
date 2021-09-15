package com.bakdata.conquery.models.auth.apitoken;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.ConqueryAuthorizationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import javax.validation.Validator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

@Slf4j
public class ApiTokenRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm {

	private final File storageDir;
	private final XodusConfig storeConfig;
	private final String storeName;
	private final Validator validator;
	private final ObjectMapper objectMapper;
	private final ArrayList<Store> openStoresInEnv = new ArrayList<>();
	private final MetaStorage storage;

	private transient Environment tokenEnvironment;
	private transient SerializingStore<byte[], ApiTokenData> tokenDataStore;
	private transient SerializingStore<byte[], ApiTokenData.MetaData> tokenMetaDataStore;

	public ApiTokenRealm(MetaStorage storage, File storageDir, XodusConfig storeConfig, String storeName, Validator validator, ObjectMapper objectMapper) {
		this.storage = storage;
		this.storageDir = storageDir;
		this.storeConfig = storeConfig;
		this.storeName = storeName;
		this.validator = validator;
		this.objectMapper = objectMapper;
		this.setAuthenticationTokenClass(ApiToken.class);
	}


	@Override
	protected void onInit() {
		super.onInit();
		// Open/create the database/store
		File passwordStoreFile = new File(storageDir, storeName);
		tokenEnvironment = Environments.newInstance(passwordStoreFile, storeConfig.createConfig());
		tokenDataStore = new SerializingStore<>(new XodusStore(tokenEnvironment,"DATA", openStoresInEnv, Environment::close, XodusStoreFactory::removeEnvironmentHook),validator, objectMapper, byte[].class, ApiTokenData.class, true, false, null);
		tokenMetaDataStore = new SerializingStore<>(new XodusStore(tokenEnvironment,"META", openStoresInEnv, Environment::close, XodusStoreFactory::removeEnvironmentHook),validator, objectMapper, byte[].class, ApiTokenData.MetaData.class, true, false, null);
	}

	@Override
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(token instanceof ApiToken)) {
			return null;
		}

		byte [] tokenHash = ApiTokenCreator.hashToken(((ApiToken) token).getCredentials());

		ApiTokenData tokenData = tokenDataStore.get(tokenHash);
		if (tokenData == null) {
			log.trace("Unknown token, cannot map token hash to token data. Aborting authentication");
			throw new IncorrectCredentialsException();
		}

		final UserId userId = tokenData.getUserId();

		final User user = storage.getUser(userId);

		if (user == null) {
			throw new UnknownAccountException("The UserId does not map to a user: " + userId);
		}

		return new ConqueryAuthenticationInfo(new UserToken(user, tokenData), token, this, false);
	}

}
