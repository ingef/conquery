package com.bakdata.conquery.models.auth.basic;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.resources.admin.rest.UserAuthenticationManagementResource;
import com.bakdata.conquery.resources.unprotected.LoginResource;
import com.bakdata.conquery.resources.unprotected.TokenResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.io.File;

@CPSType(base = AuthenticationConfig.class, id = "LOCAL_AUTHENTICATION")
@Getter
@Setter
public class LocalAuthenticationConfig implements AuthenticationConfig {
	
	/**
	 * Configuration for the password store. An encryption for the store it self might be set here.
	 */
	@NotNull
	private XodusConfig passwordStoreConfig = new XodusConfig();
	
	@Min(1)
	private int jwtDuration = 12; // Hours
	
	/**
	 * The name of the folder the store lives in.
	 */
	@NotEmpty
	private String storeName = "authenticationStore";

	
	@NotNull
	private File directory = new File("storage");
	
	@Override
	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {
		managerNode.getAuthController().getAuthenticationFilter().registerTokenExtractor(JWTokenHandler::extractToken);




		LocalAuthenticationRealm realm = new LocalAuthenticationRealm(managerNode.getStorage(), managerNode.getAuthController().getCentralTokenRealm(), storeName, directory, passwordStoreConfig);
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
