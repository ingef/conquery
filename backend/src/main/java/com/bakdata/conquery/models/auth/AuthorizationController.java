package com.bakdata.conquery.models.auth;

import java.util.*;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.conquerytoken.ConqueryTokenRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.*;
import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.Permission;
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
	private final Environment environment;
	@NonNull
	private final AuthorizationConfig authorizationConfig;
	@NonNull
	private final List<AuthenticationConfig> authenticationConfigs;
	@NonNull
	@Getter
	private final MetaStorage storage;

	@Getter
	private ConqueryTokenRealm centralTokenRealm;
	@Getter
	private List<ConqueryAuthenticationRealm> authenticationRealms = new ArrayList<>();
	@Getter
	private AuthFilter<AuthenticationToken, User> authenticationFilter;
	@Getter
	private List<Realm> realms = new ArrayList<>();

	private DefaultSecurityManager securityManager;
	
	public void init() {
		// Add the central authentication realm
		centralTokenRealm = new ConqueryTokenRealm(storage);
		authenticationRealms.add(centralTokenRealm);
		realms.add(centralTokenRealm);
		
		// Add the central authorization realm
		AuthorizingRealm authorizingRealm = new ConqueryAuthorizationRealm(storage);
		realms.add(authorizingRealm);

		// Init authentication realms provided by the config.
		for (AuthenticationConfig authenticationConf : authenticationConfigs) {
			ConqueryAuthenticationRealm realm = authenticationConf.createRealm(environment, this);
			authenticationRealms.add(realm);
			realms.add(realm);
		}


		// Register all realms in Shiro
		log.info("Registering the following realms to Shiro:\n\t{}", realms.stream().map(Realm::getName).collect(Collectors.joining("\n\t")));
		securityManager = new DefaultSecurityManager(realms);
		ModularRealmAuthenticator authenticator = (ModularRealmAuthenticator) securityManager.getAuthenticator();
		authenticator.setAuthenticationStrategy(new FirstSuccessfulStrategy());
		registerShiro();
		
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

	/**
	 * @implNote public for test purposes only
	 */
	public void registerShiro() {
		if (securityManager == null) {
			throw new IllegalStateException("The AuthorizationController was not initialized. Call init() instead");
		}
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
	private static void initializeAuthConstellation(AuthorizationConfig config, List<Realm> realms, MetaStorage storage) {
		for (ProtoUser pUser : config.getInitialUsers()) {
			pUser.registerForAuthorization(storage, true);
			for (Realm realm : realms) {
				if (realm instanceof UserManageable) {
					pUser.registerForAuthentication((UserManageable) realm, true);
				}
			}
		}
	}

	/**
	 * Creates a copy of an existing user. The copied user has the same effective permissions as the original user
	 * at the time of copying, but these are flatted. This means that the original user might hold certain permissions
	 * through inheritance from roles or groups, the copy will hold the permissions directly.
	 * @param originUserId The id of the user to make a flat copy from
	 * @param namePrefix The prefix for the id of the new copied user
	 * @return A flat copy of the referenced user
	 */
	public static User flatCopyUser(@NonNull UserId originUserId, String namePrefix, @NonNull MetaStorage storage) {
		if(Strings.isNullOrEmpty(namePrefix)) {
			throw new IllegalArgumentException("There must be a prefix");
		}

		// Find a new user id that is not used yet
		String name = null;
		do {
			name = namePrefix + UUID.randomUUID() + originUserId.getEmail();
			User prev = storage.getUser(new UserId(name));
		} while (name == null || storage.getUser(new UserId(name)) != null);

		// Retrieve original user and its effective permissions
		User origin = Objects.requireNonNull(storage.getUser(originUserId), "User to copy cannot be found");

		// Copy inherited permissions
		Set<ConqueryPermission> copiedPermission = new HashSet(AuthorizationHelper.getEffectiveUserPermissions(originUserId, storage));

		// Give read permission to all executions the original user owned
		copiedPermission.addAll(
			storage.getAllExecutions().stream()
					.filter(e -> origin.isOwner(e))
					.map(ManagedExecution::getId)
					.map(id -> QueryPermission.onInstance(Ability.READ,id))
					.collect(Collectors.toSet())
		);

		// Give read permission to all form configs the original user owned
		copiedPermission.addAll(
				storage.getAllFormConfigs().stream()
						.filter(e -> origin.isOwner(e))
						.map(FormConfig::getId)
						.map(id -> FormConfigPermission.onInstance(Ability.READ,id))
						.collect(Collectors.toSet())
		);

		// Create copied user
		User copy = new User(name, origin.getLabel());
		storage.addUser(copy);
		copy.setPermissions(storage, copiedPermission);

		return copy;
	}

	/**
	 * @see AuthorizationController#flatCopyUser(UserId, String, MetaStorage)
	 */
	public User flatCopyUser(@NonNull UserId originUserId, String namePrefix) {
		return flatCopyUser(originUserId, namePrefix, storage);
	}

}
