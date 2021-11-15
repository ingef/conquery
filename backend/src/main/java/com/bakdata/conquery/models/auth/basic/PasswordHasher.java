package com.bakdata.conquery.models.auth.basic;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.bakdata.conquery.io.jackson.Jackson;
import com.fasterxml.jackson.core.JsonProcessingException;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Inspired by @see <a href=
 * "https://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash">https://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash</a>.
 *
 */
@UtilityClass
@Slf4j
public class PasswordHasher {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final int ITERATIONS = 10000;
	private static final int KEY_LENGTH = 256;
	
	static {
		log.info(
			"Using the following settings to generate password hashes:\n\tAlgorithm: {}\n\tIterations: {}\n\tKey length: {}",
			ALGORITHM,
			ITERATIONS,
			KEY_LENGTH);
	}

	/**
	 * Returns a random salt to be used to hash a password.
	 *
	 * @return a 16 bytes random salt
	 */
	private static byte[] getNextSalt() {
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		return salt;
	}
	
	public static HashedEntry generateHashedEntry(char[] password) {
		HashedEntry entry = new HashedEntry();
		entry.setSalt(getNextSalt());
		
		entry.setHash(generateHash(password, entry.getSalt()));
		return entry;
	}

	public static byte[] generateHash(char[] password, byte[] salt) {
		PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
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
	
	@Data
	/**
	 * Container class for the entries in the store consisting of the salted password hash and the corresponding salt.
	 */
	public static class HashedEntry {
		byte[] hash;
		byte[] salt;
	}

}
