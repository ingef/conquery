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

	private final PrintableASCIIProvider tokenProvider;

	public ApiTokenCreator() {
		this(new SecureRandom());
	}

	@TestOnly
	public ApiTokenCreator(Random random) {
		tokenProvider = new PrintableASCIIProvider(random);
	}



	public ApiToken createToken(){
		CharArrayBuffer buffer = new CharArrayBuffer(TOKEN_PREFIX.length() + "_".length() + TOKEN_LENGTH);
		buffer.append(TOKEN_PREFIX);
		buffer.append('_');
		tokenProvider.fillRemaining(buffer);
		return new ApiToken(buffer);
	}
}
