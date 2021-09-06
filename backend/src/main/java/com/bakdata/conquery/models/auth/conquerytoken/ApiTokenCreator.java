package com.bakdata.conquery.models.auth.conquerytoken;

import lombok.Data;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

@Data
public class ApiTokenCreator {
	public static final int TOKEN_LENGTH = 30;
	public static final String TOKEN_PREFIX = "cq"; // short for conquery

	private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final int ITERATIONS = 10000;
	private static final int KEY_LENGTH = 256;
	private static final byte[] SALT = {'s','t','a','t','i','c','_','s','a','l','t'};

	private final PrintableASCIIProvider tokenProvider;

	public ApiTokenCreator(Random random) {
		tokenProvider = new PrintableASCIIProvider(new SecureRandom());
	}



	public String createToken(){
		return TOKEN_PREFIX + "_" + tokenProvider.getString(TOKEN_LENGTH);
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
