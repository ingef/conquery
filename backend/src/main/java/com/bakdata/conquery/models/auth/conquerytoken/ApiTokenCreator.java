package com.bakdata.conquery.models.auth.conquerytoken;

import lombok.Data;
import org.apache.commons.lang3.CharSet;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

@Data
public class ApiTokenCreator {
	private static final int TOKEN_LENGTH = 30;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final int ITERATIONS = 10000;
	private static final int KEY_LENGTH = 256;
	private static final byte[] SALT = {'s','t','a','t','i','c','_','s','a','l','t'};


	private final String tokenPrefix;

	public String createToken(){
		byte [] token = new byte[TOKEN_LENGTH *];
		RANDOM.nextBytes();
		return tokenPrefix + "_" + "randomHash";
	}

	public byte[] hashToken(char[] apiToken){
		PBEKeySpec spec = new PBEKeySpec(apiToken, SALT, ITERATIONS, KEY_LENGTH);
		SecretKeyFactory f = null;
		try {
			f = SecretKeyFactory.getInstance(ALGORITHM);
			return f.generateSecret(spec).getEncoded();
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("The indicated algorithm was not found", e);
		}
		catch (InvalidKeySpecException e) {
			throw new IllegalStateException("The key specification was invalid", e);
		}
	}
}
