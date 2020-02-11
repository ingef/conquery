package com.bakdata.conquery.models.auth.basic;

import java.security.SecureRandom;
import java.util.Random;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.config.XodusConfig;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

@CPSType(base = AuthenticationConfig.class, id = "LOCAL_AUTHENTICATION")
@Getter
@Setter
public class LocalAuthenticationConfig implements AuthenticationConfig {
	
	private static final Random RANDOM = new SecureRandom();
	/**
	 * The secret to sign the created JWTs.
	 */
	@NotEmpty
	private String tokenSecret = generateToken();
	
	/**
	 * Configuration for the password store. An encryption for the store it self might be set here.
	 */
	@NotNull
	private XodusConfig passwordStoreConfig = new XodusConfig();
	
	/**
	 * The name of the folder the store lives in.
	 */
	@NotEmpty
	private String storeName = "authenticationStore";
	
	@Override
	public LocalAuthenticationRealm createRealm(MasterMetaStorage storage) {
		return new LocalAuthenticationRealm(storage, this);
	}
	
	/**
	 * Generate a random default token.
	 * @return The token as a {@link String}
	 */
	private static String generateToken() {
		byte[] buffer = new byte[32];
		RANDOM.nextBytes(buffer);
		return buffer.toString();
	}
}
