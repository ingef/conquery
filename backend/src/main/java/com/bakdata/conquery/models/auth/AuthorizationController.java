package com.bakdata.conquery.models.auth;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import io.dropwizard.auth.Authenticator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;

@Slf4j
public class AuthorizationController {
	@Getter
	private Authenticator<AuthenticationToken, User> authenticator;
	@Getter
	private MasterMetaStorage storage;
	@Getter
	private List<ConqueryRealm> realms;
	
	
	private static AuthorizationController INSTANCE = null;
	
	private AuthorizationController(
		MasterMetaStorage storage,
		List<ConqueryRealm> realms, 
		Authenticator<AuthenticationToken, User> authenticator) {
		this.authenticator = authenticator;
		this.storage = storage;
		this.realms = realms;
		AuthorizingRealm authorizingRealm = new ConqueryAuthorizationRealm(storage);
		List<Realm> allRealms = new ArrayList<>(realms);
		allRealms.add(authorizingRealm);

		SecurityManager securityManager = new DefaultSecurityManager(allRealms);
		SecurityUtils.setSecurityManager(securityManager);
		log.debug("Security manager registered");
	}
	
	public static AuthorizationController getInstance() {
		if(INSTANCE == null) {
			throw new IllegalStateException(String.format("%s has not been initialized yet.", AuthorizationController.class.getSimpleName()));
		}
		return INSTANCE;
	}
	
	
	public static void init(ConqueryConfig config, MasterMetaStorage storage) {
		AuthConfig authConfig = config.getAuthentication();
	
		
		INSTANCE =  new AuthorizationController(storage, authConfig.getRealms(), new ConqueryAuthenticator(storage));
	}
}
