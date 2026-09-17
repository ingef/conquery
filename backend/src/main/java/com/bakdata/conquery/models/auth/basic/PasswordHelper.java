package com.bakdata.conquery.models.auth.basic;

import java.util.Map;
import java.util.function.Function;

import com.password4j.Argon2Function;
import com.password4j.BcryptFunction;
import com.password4j.CompressedPBKDF2Function;
import com.password4j.HashingFunction;
import com.password4j.ScryptFunction;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class PasswordHelper {

	private final static Map<Class<? extends HashingFunction>, Function<String, HashingFunction>> HASH_FUNCTION_GENERATORS = Map.of(
			Argon2Function.class, Argon2Function::getInstanceFromHash,
			ScryptFunction.class, ScryptFunction::getInstanceFromHash,
			BcryptFunction.class, BcryptFunction::getInstanceFromHash,
			CompressedPBKDF2Function.class, CompressedPBKDF2Function::getInstanceFromHash
	);

	/**
	 * Determines the function used to create the provided hash.
	 * The function can be used to hash a plain credential in order to check the hashes for equality.
	 */
	public HashingFunction getHashingFunction(String hash) {
		for (Map.Entry<Class<? extends HashingFunction>, Function<String, HashingFunction>> hashFunctionGenerator : HASH_FUNCTION_GENERATORS.entrySet()) {
			try {
				return hashFunctionGenerator.getValue().apply(hash);
			}
			catch (Exception e) {
				log.trace("Could not create hash function instance from hash using '{}'", hashFunctionGenerator.getKey());
			}
		}
		throw new IllegalArgumentException("No supported hash function recognized hash. Supported functions: " + HASH_FUNCTION_GENERATORS.keySet());
	}
}
