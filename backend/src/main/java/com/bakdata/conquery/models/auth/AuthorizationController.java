package com.bakdata.conquery.models.auth;

import java.util.ArrayList;
import java.util.List;

import javax.activation.UnsupportedDataTypeException;

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
import org.apache.shiro.util.LifecycleUtils;

@Slf4j
public class AuthorizationController {
	@Getter
	private Authenticator<AuthenticationToken, User> authenticator;
	@Getter
	private MasterMetaStorage storage;
	@Getter
	private List<ConqueryAuthenticationRealm> authenticationRealms;

	private List<Realm> realms;
	
	
	private static AuthorizationController INSTANCE = null;
	
	private AuthorizationController(
		MasterMetaStorage storage,
		ConqueryConfig config,
		Authenticator<AuthenticationToken, User> authenticator) {
		this.authenticator = authenticator;
		this.storage = storage;
		realms = new ArrayList<>(realms);
		
		// Init authentication realms provided by with the config.
		for(AuthenticationConfig authenticationConf : config.getAuthentication()) {
			ConqueryAuthenticationRealm realm = authenticationConf.createRealm(storage);
			
			// Register user if realm supports it
			if(realm instanceof UserManageable) {
				// TODO ...
			}
			authenticationRealms.add(realm);
			if(!(realm instanceof Realm)) {
				throw new UnsupportedDataTypeException("");
			}
			realms.add(realm);
		}
		AuthorizingRealm authorizingRealm = new ConqueryAuthorizationRealm(storage);
		realms.add(authorizingRealm);
		
		// Call Shiros init on all realms
		realms.stream().forEach(LifecycleUtils::init);
		
		// Register all realms in Shiro
		SecurityManager securityManager = new DefaultSecurityManager(realms);
		SecurityUtils.setSecurityManager(securityManager);
		log.debug("Security manager registered");
	}
	
}
