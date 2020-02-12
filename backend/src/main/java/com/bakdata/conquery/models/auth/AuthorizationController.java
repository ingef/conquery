package com.bakdata.conquery.models.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import io.dropwizard.auth.AuthFilter;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
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
public final class AuthorizationController {
	
	@NonNull
	private final AuthorizationConfig authorizationConfig;
	@NonNull
	private final List<AuthenticationConfig> authenticationConfigs;
	@NonNull
	@Getter
	private final MasterMetaStorage storage;

	@Getter
	private List<ConqueryAuthenticationRealm> authenticationRealms = new ArrayList<>();
	@Getter
	private AuthFilter<AuthenticationToken, User> authenticationFilter;
	@Getter
	private List<Realm> realms = new ArrayList<>();

	public void init() {

		initializeRealms(storage, authenticationConfigs, authenticationRealms, realms);

		registerShiro(realms);

		// Create Jersey filter for authentication
		this.authenticationFilter = DefaultAuthFilter.asDropwizardFeature(this);

		// Register initial users for authorization and authentication (if the realm is able to)
		initializeAuthConstellation(authorizationConfig, realms, storage);
	}

	private static void registerShiro(List<Realm> realms) {
		// Register all realms in Shiro
		log.info("Registering the following realms to Shiro:\n\t", realms.stream().map(Realm::getName).collect(Collectors.joining("\n\t")));
		SecurityManager securityManager = new DefaultSecurityManager(realms);
		SecurityUtils.setSecurityManager(securityManager);
		log.debug("Security manager registered");
	}

	private static void initializeRealms(MasterMetaStorage storage, List<AuthenticationConfig> config, List<ConqueryAuthenticationRealm> authenticationRealms, List<Realm> realms) {
		// Init authentication realms provided by with the config.
		for (AuthenticationConfig authenticationConf : config) {
			ConqueryAuthenticationRealm realm = authenticationConf.createRealm(storage);
			authenticationRealms.add(realm);
			realms.add(realm);
		}
		AuthorizingRealm authorizingRealm = new ConqueryAuthorizationRealm(storage);
		realms.add(authorizingRealm);

		// Call Shiros init on all realms
		realms.stream().forEach(LifecycleUtils::init);
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
