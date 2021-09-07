package com.bakdata.conquery.models.auth.apitoken;

import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.SimpleStoreInfo;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.io.File;
import java.util.ArrayList;

public class ApiTokenRealm extends AuthorizingRealm {

	private final File storageDir;

	private final XodusConfig storeConfig;
	private final String storeName;

	@JsonIgnore
	private transient Environment tokenEnvironment;
	@JsonIgnore
	private transient SerializingStore tokenDataStore;
	@JsonIgnore
	private transient SerializingStore tokenMetaDataStore;

	private final transient ArrayList<Store> openStoresInEnv = new ArrayList<>();

	public ApiTokenRealm(File storageDir, XodusConfig storeConfig, String storeName) {
		this.storageDir = storageDir;
		this.storeConfig = storeConfig;
		this.storeName = storeName;
		this.setAuthenticationTokenClass(ApiToken.class);
	}


	@Override
	protected void onInit() {
		super.onInit();
		// Open/create the database/store
		File passwordStoreFile = new File(storageDir, storeName);
		tokenEnvironment = Environments.newInstance(passwordStoreFile, storeConfig.createConfig());
//		tokenDataStore = new XodusStore(tokenEnvironment, new SimpleStoreInfo("data", byte[].class, ApiTokenData.class), openStoresInEnv, XodusStoreFactory::removeEnvironment);
//		tokenMetaDataStore = new XodusStore(tokenEnvironment, new SimpleStoreInfo("data", byte[].class, ApiTokenData.MetaData.class), openStoresInEnv, XodusStoreFactory::removeEnvironment);
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(token instanceof ApiToken)) {
			return null;
		}

		byte [] tokenHash = ApiTokenCreator.hashToken(((ApiToken) token).getCredentials());

		//tokenEnvironment.computeInReadonlyTransaction(t -> tokenDataStore.get())

		// TODO return useful stuff
		return null;
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		return null;
	}




}
