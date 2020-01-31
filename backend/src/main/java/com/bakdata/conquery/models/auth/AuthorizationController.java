package com.bakdata.conquery.models.auth;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import io.dropwizard.auth.AuthFilter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.util.LifecycleUtils;

@Slf4j
public class AuthorizationController {
	@Getter
	private MasterMetaStorage storage;
	@Getter
	private List<ConqueryAuthenticationRealm> authenticationRealms = new ArrayList<>();
	@Getter
	AuthFilter<AuthenticationToken, User> authenticationFilter;
	@Getter
	private List<Realm> realms = new ArrayList<>();
	
	public AuthorizationController(MasterMetaStorage storage, ConqueryConfig config) {
		this.storage = storage;
		
		// Init authentication realms provided by with the config.
		for(AuthenticationConfig authenticationConf : config.getAuthentication()) {
			ConqueryAuthenticationRealm realm = authenticationConf.createRealm(storage);
			
			// Register user if realm supports it
			if(realm instanceof UserManageable) {
				// TODO ...
			}
			authenticationRealms.add(realm);
			if (!(realm instanceof Realm)) {
				throw new IllegalStateException(String.format(
					"For this application objects of classes that implement %s must also extend %s. The object %s doesn't.",
					ConqueryAuthenticationRealm.class.getName(),
					AuthenticatingRealm.class.getName(),
					realm));
			}
			realms.add((Realm) realm);
		}
		AuthorizingRealm authorizingRealm = new ConqueryAuthorizationRealm(storage);
		realms.add(authorizingRealm);
		
		// Call Shiros init on all realms
		realms.stream().forEach(LifecycleUtils::init);
		
		// Register all realms in Shiro
		SecurityManager securityManager = new DefaultSecurityManager(realms);
		SecurityUtils.setSecurityManager(securityManager);
		log.debug("Security manager registered");
		
		// Create Jersey filter for authentication
		authenticationFilter = DefaultAuthFilter.asDropwizardFeature(this);
		
		
		// Register initial users for authorization and authentication (if the realm is able to)
		initializeAuthConstellation(config.getAuthorization(), realms, storage);
	}
	
	/**
	 * Sets up the initial subjects and permissions for the authentication system.
	 * @param storage A storage, where the handler might add a new users.
	 */
	private static void initializeAuthConstellation(AuthorizationConfig config, List<Realm> realms, MasterMetaStorage storage) {
		for (ProtoUser pUser : config.getInitialUsers()) {
			pUser.registerForAuthorization(storage);
			for (Realm realm : realms) {
				if (realm instanceof UserManageable) {
					pUser.registerForAuthentication((UserManageable) realm);
				}
			}
		}
	}
	
}
