package com.bakdata.conquery.models.auth.apitoken;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Joiner;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * An erasable hash for tokens that implements equals based on its contents
 */
@Data
@RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
public class ApiTokenHash {
	private final byte[] hash;

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!ApiTokenHash.class.isAssignableFrom(obj.getClass())) {
			return false;
		}
		return Arrays.equals(hash, ((ApiTokenHash) obj).hash);
	}

	public int hashCode() {
		return Arrays.hashCode(hash);
	}

	public void clear() {
		Arrays.fill(hash, (byte) 0);
	}
}
