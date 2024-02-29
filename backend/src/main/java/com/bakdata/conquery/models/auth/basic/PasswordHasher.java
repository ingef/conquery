package com.bakdata.conquery.models.auth.basic;

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

	@Data
	/**
	 * Container class for the entries in the store consisting of the salted password hash and the corresponding salt.
	 */
	public static class HashEntry {
		final String hash;
	}

}
