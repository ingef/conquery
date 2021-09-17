package com.bakdata.conquery.models.auth.apitoken;

import lombok.Data;
import org.apache.http.util.CharArrayBuffer;
import org.jetbrains.annotations.TestOnly;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

/**
 * Provider for random generated API tokens.
 *
 * Use a pseudo random generator for test purpose only.
 */
@Data
public class ApiTokenCreator {
	public static final int TOKEN_LENGTH = 37; // GitHub uses 37 alphanumerics for their token
	public static final String TOKEN_PREFIX = "cq"; // short for conquery

	private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final int ITERATIONS = 10000;
	private static final int KEY_LENGTH = 256;
	private static final byte[] SALT = {'s','t','a','t','i','c','_','s','a','l','t'};

	private final PrintableASCIIProvider tokenProvider;

	public ApiTokenCreator() {
		this(new SecureRandom());
	}

	@TestOnly
	public ApiTokenCreator(Random random) {
		tokenProvider = new PrintableASCIIProvider(random);
	}



	public CharArrayBuffer createToken(){
		CharArrayBuffer buffer = new CharArrayBuffer(TOKEN_PREFIX.length() + "_".length() + TOKEN_LENGTH);
		buffer.append(TOKEN_PREFIX);
		buffer.append('_');
		tokenProvider.fillRemaining(buffer);
		return buffer;
	}

	public static byte[] hashToken(CharArrayBuffer apiToken){
		PBEKeySpec spec = new PBEKeySpec(apiToken.buffer(), SALT, ITERATIONS, KEY_LENGTH);
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
