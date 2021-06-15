package com.bakdata.conquery.models.config.auth;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.JWTokenHandler;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.UserAuthenticationManagementProcessor;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.resources.admin.rest.UserAuthenticationManagementResource;
import com.bakdata.conquery.resources.unprotected.LoginResource;
import com.bakdata.conquery.resources.unprotected.TokenResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@CPSType(base = AuthenticationConfig.class, id = "LOCAL_AUTHENTICATION")
@Getter
@Setter
public class LocalAuthenticationConfig implements AuthenticationConfig {
	
	/**
	 * Configuration for the password store. An encryption for the store it self might be set here.
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
		if(passwordStoreConfig.getCipherId() != null){
			return true;
		}

		// and system property
		return System.getProperty("exodus.cipherId") != null;
	}
	
	@Override
	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {
		// Token extractor is not needed because this realm depends on the ConqueryTokenRealm
		managerNode.getAuthController().getAuthenticationFilter().registerTokenExtractor(JWTokenHandler::extractToken);


		LocalAuthenticationRealm realm = new LocalAuthenticationRealm(
				managerNode.getAuthController().getConqueryTokenRealm(),
				storeName,
				directory,
				passwordStoreConfig,
				jwtDuration);
		UserAuthenticationManagementProcessor processor = new UserAuthenticationManagementProcessor(realm, managerNode.getStorage());

		// Register resources for users to exchange username and password for an access token
		registerAdminUnprotectedAuthenticationResources(managerNode.getUnprotectedAuthAdmin(), realm);
		registerApiUnprotectedAuthenticationResources(managerNode.getUnprotectedAuthApi(), realm);

		registerAuthenticationAdminResources(managerNode.getAdmin().getJerseyConfig(), processor);
		return realm;
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
		jerseyConfig.register(new AbstractBinder() {

			@Override
			protected void configure() {
				this.bind(userProcessor).to(UserAuthenticationManagementProcessor.class);
			}

		});
		jerseyConfig.register(UserAuthenticationManagementResource.class);
	}
}
