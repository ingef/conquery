package com.bakdata.conquery.models.auth;

import java.util.*;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.auth.ProtoRole;
import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.basic.JWTokenHandler;
import com.bakdata.conquery.models.auth.conquerytoken.ConqueryTokenRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.web.AuthFilter;
import com.bakdata.conquery.models.auth.web.RedirectingAuthFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.auth.AuthenticationRealmFactory;
import com.bakdata.conquery.models.config.auth.AuthorizationConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.AdminServlet;
import com.bakdata.conquery.resources.unprotected.AuthServlet;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.lifecycle.Managed;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.util.LifecycleUtils;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

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
public final class AuthorizationController implements Managed {

	@NonNull
	private final ConqueryConfig config;
	@NonNull
	private final Environment environment;
	@NonNull
	@Getter
	private final MetaStorage storage;
	@Getter
	private final AdminServlet adminServlet;
	@Getter
	private final ConqueryTokenRealm conqueryTokenRealm;
	@Getter
	private final List<ConqueryAuthenticationRealm> authenticationRealms = new ArrayList<>();
	@Getter
	private final List<Realm> realms = new ArrayList<>();

	private final DefaultSecurityManager securityManager;


	// Resources without authentication
	@Getter
	private DropwizardResourceConfig unprotectedAuthApi;
	@Getter
	private DropwizardResourceConfig unprotectedAuthAdmin;

	public AuthorizationController(MetaStorage storage, ConqueryConfig config, Environment environment, AdminServlet adminServlet) {
		this.storage = storage;
		this.config = config;
		this.environment = environment;
		this.adminServlet = adminServlet;

		if (adminServlet != null) {
			adminServlet.getJerseyConfig().register(AuthFilter.class);
			AuthFilter.registerTokenExtractor(JWTokenHandler.JWTokenExtractor.class, adminServlet.getJerseyConfig());

			// The binding is necessary here because the RedirectingAuthFitler delegates to the DefaultAuthfilter at the moment
			adminServlet.getJerseyConfigUI().register(new AbstractBinder() {
				@Override
				protected void configure() {
					bindAsContract(AuthFilter.class);
				}
			});
			adminServlet.getJerseyConfigUI().register(RedirectingAuthFilter.class);
			AuthFilter.registerTokenExtractor(JWTokenHandler.JWTokenExtractor.class, adminServlet.getJerseyConfigUI());
		}

		unprotectedAuthAdmin = AuthServlet.generalSetup(environment.metrics(), config, environment.admin(), environment.getObjectMapper());
		unprotectedAuthApi = AuthServlet.generalSetup(environment.metrics(), config, environment.servlets(), environment.getObjectMapper());


		// Add the user token realm
		conqueryTokenRealm = new ConqueryTokenRealm(storage);
		authenticationRealms.add(conqueryTokenRealm);
		realms.add(conqueryTokenRealm);

		// Add the central authorization realm
		final AuthorizingRealm authorizingRealm = new ConqueryAuthorizationRealm(storage);
		realms.add(authorizingRealm);

		securityManager = new DefaultSecurityManager(realms);
		final ModularRealmAuthenticator authenticator = (ModularRealmAuthenticator) securityManager.getAuthenticator();
		authenticator.setAuthenticationStrategy(new FirstSuccessfulStrategy());

		registerStaticSecurityManager();

	}

	/**
	 * @implNote public for test purposes only
	 */
	public void registerStaticSecurityManager() {
		if (securityManager == null) {
			throw new IllegalStateException("The AuthorizationController was not initialized. Call init() instead");
		}
		SecurityUtils.setSecurityManager(securityManager);
		log.debug("Security manager registered");
	}

	@Override
	public void start() throws Exception {

		externalInit();

		// Call Shiros init on all realms
		LifecycleUtils.init(realms);

		// Register initial users for authorization and authentication (if the realm is able to)
		initializeAuthConstellation(config.getAuthorizationRealms(), realms, storage);
	}

	private void externalInit() {


		// Init authentication realms provided by the config.
		for (AuthenticationRealmFactory authenticationConf : config.getAuthenticationRealms()) {
			final ConqueryAuthenticationRealm realm = authenticationConf.createRealm(environment, config, this);
			authenticationRealms.add(realm);
			realms.add(realm);
		}

		// Register all realms in Shiro
		log.info("Registering the following realms to Shiro:\n\t{}", realms.stream().map(Realm::getName).collect(Collectors.joining("\n\t")));
		securityManager.setRealms(realms);
	}

	/**
	 * Sets up the initial subjects and permissions for the authentication system
	 * that are found in the config.
	 *
	 * @param storage A storage, where the handler might add a new users.
	 */
	private static void initializeAuthConstellation(@NonNull AuthorizationConfig config, @NonNull List<Realm> realms, @NonNull MetaStorage storage) {
		for (ProtoRole pRole : config.getInitialRoles()) {
			pRole.createOrOverwriteRole(storage);
		}

		for (ProtoUser pUser : config.getInitialUsers()) {

			final User user = pUser.createOrOverwriteUser(storage);

			for (Realm realm : realms) {
				if (realm instanceof UserManageable) {
					AuthorizationHelper.registerForAuthentication((UserManageable) realm, user, pUser.getCredential(), true);
				}
			}
		}
	}

	@Override
	public void stop() throws Exception {
		LifecycleUtils.destroy(authenticationRealms);
	}

	/**
	 * @see AuthorizationController#flatCopyUser(User, String, MetaStorage)
	 */
	public User flatCopyUser(@NonNull User originUser, String namePrefix) {
		return flatCopyUser(originUser, namePrefix, storage);
	}

	/**
	 * Creates a copy of an existing user. The copied user has the same effective permissions as the original user
	 * at the time of copying, but these are flatted. This means that the original user might hold certain permissions
	 * through inheritance from roles or groups, the copy will hold the permissions directly.
	 *
	 * @param originUser The user to make a flat copy of
	 * @param namePrefix The prefix for the id of the new copied user
	 * @return A flat copy of the referenced user
	 */
	public static User flatCopyUser(@NonNull User originUser, String namePrefix, @NonNull MetaStorage storage) {
		final UserId originUserId = originUser.getId();

		if (Strings.isBlank(namePrefix)) {
			throw new IllegalArgumentException("There must be a prefix");
		}

		// Find a new user id that is not used yet
		String name = null;
		do {
			name = namePrefix + UUID.randomUUID() + originUserId.getName();
		} while (storage.getUser(new UserId(name)) != null);

		// Retrieve original user and its effective permissions

		// Copy inherited permissions
		final Set<ConqueryPermission> copiedPermission = new HashSet<>();

		// This collects all permissions from the user, its groups and inherited roles
		copiedPermission.addAll(originUser.getEffectivePermissions());

		// Give read permission to all executions the original user owned
		copiedPermission.addAll(
				storage.getAllExecutions()
					   .filter(originUser::isOwner)
					   .map(exc -> exc.createPermission(Ability.READ.asSet()))
					   .collect(Collectors.toSet())
		);

		// Give read permission to all form configs the original user owned
		copiedPermission.addAll(
				storage.getAllFormConfigs()
					   .filter(originUser::isOwner)
					   .map(conf -> conf.createPermission(Ability.READ.asSet()))
					   .collect(Collectors.toSet())
		);

		// Create copied user
		final User copy = new User(name, originUser.getLabel());
		copy.setMetaStorage(storage);
		storage.addUser(copy);
		copy.updatePermissions(copiedPermission);

		return copy;
	}

}
