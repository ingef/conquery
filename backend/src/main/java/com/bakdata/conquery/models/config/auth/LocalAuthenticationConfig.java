package com.bakdata.conquery.models.config.auth;

import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.bakdata.conquery.apiv1.RequestHelper;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.JWTokenHandler;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.UserAuthenticationManagementProcessor;
import com.bakdata.conquery.models.auth.web.AuthFilter;
import com.bakdata.conquery.models.auth.web.RedirectingAuthFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.resources.admin.AdminServlet;
import com.bakdata.conquery.resources.admin.rest.UserAuthenticationManagementResource;
import com.bakdata.conquery.resources.unprotected.LoginResource;
import com.bakdata.conquery.resources.unprotected.TokenResource;
import com.password4j.BcryptFunction;
import com.password4j.BenchmarkResult;
import com.password4j.SystemChecker;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@CPSType(base = AuthenticationRealmFactory.class, id = "LOCAL_AUTHENTICATION")
@Getter
@Setter
@Slf4j
public class LocalAuthenticationConfig implements AuthenticationRealmFactory {

	public static final int BCRYPT_MAX_MILLISECONDS = 300;
	/**
	 * Configuration for the password store. An encryption for the store itself might be set here.
	 */
	@NotNull
	private XodusConfig passwordStoreConfig = new XodusConfig();

	@MinDuration(value = 1, unit = TimeUnit.MINUTES)
	private Duration jwtDuration = Duration.hours(12);
	
	/**
	 * The name of the folder the store lives in.
	 */
	@NotEmpty
	private String storeName = "authenticationStore";

	
	@NotNull
	private File directory = new File("storage");


	@ValidationMethod(message = "Storage has no encryption configured")
	boolean isStorageEncrypted() {
		// Check if a cipher is configured for xodus according to https://github.com/JetBrains/xodus/wiki/Database-Encryption
		// in the config
		if (passwordStoreConfig.getCipherId() != null) {
			return true;
		}

		// and system property
		return System.getProperty("exodus.cipherId") != null;
	}
	
	@Override
	public ConqueryAuthenticationRealm createRealm(Environment environment, ConqueryConfig config, AuthorizationController authorizationController) {
		// Token extractor is not needed because this realm depends on the ConqueryTokenRealm
		AuthFilter.registerTokenExtractor(JWTokenHandler.JWTokenExtractor.class, environment.jersey().getResourceConfig());

		log.info("Performing benchmark for default hash function (bcrypt) with max_milliseconds={}", BCRYPT_MAX_MILLISECONDS);
		final BenchmarkResult<BcryptFunction> result = SystemChecker.benchmarkBcrypt(BCRYPT_MAX_MILLISECONDS);

		final BcryptFunction prototype = result.getPrototype();


		log.info("Using bcrypt with {} logarithmic rounds. Elapsed time={}", prototype.getLogarithmicRounds(), result.getElapsed());

		LocalAuthenticationRealm realm = new LocalAuthenticationRealm(
				environment.getValidator(),
				Jackson.copyMapperAndInjectables(Jackson.BINARY_MAPPER),
				authorizationController.getConqueryTokenRealm(),
				storeName,
				directory,
				passwordStoreConfig,
				jwtDuration,
				prototype
		);
		UserAuthenticationManagementProcessor processor = new UserAuthenticationManagementProcessor(realm, authorizationController.getStorage());

		// Register resources for users to exchange username and password for an access token
		registerAdminUnprotectedAuthenticationResources(authorizationController.getUnprotectedAuthAdmin(), realm);
		registerApiUnprotectedAuthenticationResources(authorizationController.getUnprotectedAuthApi(), realm);

		registerAuthenticationAdminResources(authorizationController.getAdminServlet().getJerseyConfig(), processor);

		// Add login schema for admin end
//TODO		redirectingAuthFilter.getLoginInitiators().add(loginProvider(authorizationController.getUnprotectedAuthAdmin()));

		return realm;
	}

	private Function<ContainerRequestContext,URI> loginProvider(DropwizardResourceConfig unprotectedAuthAdmin) {
		return (ContainerRequestContext request) -> {
			return UriBuilder.fromPath(unprotectedAuthAdmin.getUrlPattern())
							 .path(LoginResource.class)
							 .queryParam(RedirectingAuthFilter.REDIRECT_URI, UriBuilder.fromUri(RequestHelper.getRequestURL(request))
																					   .path(AdminServlet.ADMIN_UI)
																					   .build())
							 .build();
		};

	}

	//////////////////// RESOURCE REGISTRATION ////////////////////
	public void registerAdminUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig, LocalAuthenticationRealm realm) {
		jerseyConfig.register(new TokenResource(realm));
		jerseyConfig.register(LoginResource.class);
	}

	public void registerApiUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig, LocalAuthenticationRealm realm) {
		jerseyConfig.register(new TokenResource(realm));
	}

	public void registerAuthenticationAdminResources(DropwizardResourceConfig jerseyConfig, UserAuthenticationManagementProcessor userProcessor) {
		jerseyConfig.register(userProcessor);

		jerseyConfig.register(UserAuthenticationManagementResource.class);
	}
}
