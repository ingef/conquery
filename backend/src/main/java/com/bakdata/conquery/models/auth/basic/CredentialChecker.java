package com.bakdata.conquery.models.auth.basic;

import java.util.Arrays;

import com.bakdata.conquery.io.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.basic.PasswordHasher.HashedEntry;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.StringBinding;
import lombok.experimental.UtilityClass;
import org.apache.shiro.authc.IncorrectCredentialsException;

@UtilityClass
public class CredentialChecker {


	/**
	 * Checks if the provided username password combination is valid.
	 * This is done by checking the password's hash with the stored hash.
	 * NOTE: After this operation the provided password is cleared.
	 * @param username The submitted username.
	 * @param providedPassword The submitted password
	 * @param passwordStore The store that holds the hashed passwords.
	 * @return True if the username-password combination is valid.
	 */
	public static boolean validUsernamePassword(String username, char[] providedPassword, XodusStore passwordStore) {
		try {			
			if(username.isEmpty()) {
				throw new IncorrectCredentialsException("Username was empty");
			}
			if(providedPassword.length < 1) {
				throw new IncorrectCredentialsException("Password was empty");			
			}
			ByteIterable storedHashedEntry = passwordStore.get(StringBinding.stringToEntry(username));
			if(storedHashedEntry == null) {
				return false;
			}
			HashedEntry hashedEntry = HashedEntry.fromByteIterable(storedHashedEntry);
			return isCredentialValid(providedPassword, hashedEntry);
		}
		finally {
			// Erase the provided password
			Arrays.fill(providedPassword, '\0');
		}
	}

	public static boolean isCredentialValid(char[] providedCredentials, HashedEntry hashedEntry) {
		byte[] hashFromProvided = PasswordHasher.generateHash(providedCredentials, hashedEntry.getSalt()); 
		return Arrays.equals(hashFromProvided, hashedEntry.getHash());
	}
}
