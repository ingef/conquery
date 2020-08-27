package com.bakdata.conquery.models.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.conquerytoken.ConqueryTokenRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.lifecycle.Managed;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.util.LifecycleUtils;

/**
 * The central class for the initialization of authorization and authentication.
 * Conquery uses a permission based authorization and supports different type of
 * authentication. For each authentication type a
 * {@link ConqueryAuthenticationRealm} must be defined and its configuration
 * needs to be appended in the {@link ConqueryConfig}. A single
 * {@link ConqueryAuthorizationRealm} handles the mapping of the authenticated
 * {@link UserId}s to the permissions they hold.
 */
@Slf4j
@RequiredArgsConstructor
public final class AuthorizationController implements Managed{
	
	public static AuthorizationController INSTANCE;
	
	@NonNull
	private final AuthorizationConfig authorizationConfig;
	@NonNull
	private final List<AuthenticationConfig> authenticationConfigs;
	@NonNull
	@Getter
	private final MasterMetaStorage storage;

	@Getter
	private ConqueryTokenRealm centralTokenRealm;
	@Getter
	private List<ConqueryAuthenticationRealm> authenticationRealms = new ArrayList<>();
	@Getter
	private AuthFilter<AuthenticationToken, User> authenticationFilter;
	@Getter
	private List<Realm> realms = new ArrayList<>();
	
	public void init() {
		// Add the central authentication realm
		centralTokenRealm = new ConqueryTokenRealm(storage);
		authenticationRealms.add(centralTokenRealm);
		realms.add(centralTokenRealm);
		
		// Add the central authorization realm
		AuthorizingRealm authorizingRealm = new ConqueryAuthorizationRealm(storage);
		realms.add(authorizingRealm);

		// Init authentication realms provided by with the config.
		for (AuthenticationConfig authenticationConf : authenticationConfigs) {
			ConqueryAuthenticationRealm realm = authenticationConf.createRealm(this);
			authenticationRealms.add(realm);
			realms.add(realm);
		}
		
		registerShiro(realms);
		
		// Create Jersey filter for authentication
		this.authenticationFilter = DefaultAuthFilter.asDropwizardFeature(this);

		INSTANCE = this;
	}
	
	@Override
	public void start() throws Exception {
		// Call Shiros init on all realms
		LifecycleUtils.init(realms);
		// Register initial users for authorization and authentication (if the realm is able to)
		initializeAuthConstellation(authorizationConfig, realms, storage);
	}
	
	@Override
	public void stop() throws Exception {
		LifecycleUtils.destroy(authenticationRealms);
	}
	
	private static void registerShiro(List<Realm> realms) {
		// Register all realms in Shiro
		log.info("Registering the following realms to Shiro:\n\t{}", realms.stream().map(Realm::getName).collect(Collectors.joining("\n\t")));
		DefaultSecurityManager securityManager = new DefaultSecurityManager(realms);
		ModularRealmAuthenticator authenticator = (ModularRealmAuthenticator) securityManager.getAuthenticator();
		authenticator.setAuthenticationStrategy(new FirstSuccessfulStrategy());
		SecurityUtils.setSecurityManager(securityManager);
		log.debug("Security manager registered");
	}

	/**
	 * Sets up the initial subjects and permissions for the authentication system
	 * that are found in the config.
	 * 
	 * @param storage
	 *            A storage, where the handler might add a new users.
	 */
	private static void initializeAuthConstellation(AuthorizationConfig config, List<Realm> realms, MasterMetaStorage storage) {
		for (ProtoUser pUser : config.getInitialUsers()) {
			pUser.registerForAuthorization(storage, true);
			for (Realm realm : realms) {
				if (realm instanceof UserManageable) {
					pUser.registerForAuthentication((UserManageable) realm, true);
				}
			}
		}
	}

}
