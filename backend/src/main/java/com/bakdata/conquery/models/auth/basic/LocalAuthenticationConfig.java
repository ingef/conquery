package com.bakdata.conquery.models.auth.basic;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.config.XodusConfig;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.Setter;

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
	public ConqueryAuthenticationRealm createRealm(Environment environment, AuthorizationController controller) {
		return new LocalAuthenticationRealm(controller, this);
	}
}
