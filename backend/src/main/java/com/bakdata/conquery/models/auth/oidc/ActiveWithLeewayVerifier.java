package com.bakdata.conquery.models.auth.oidc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.exceptions.TokenNotActiveException;
import org.keycloak.representations.JsonWebToken;

/**
 * Allows tokens a leeway (a short valid period of time after they are actually expired).
 */
public class ActiveWithLeewayVerifier implements TokenVerifier.Predicate<JsonWebToken> {

	private final int leeway;

	public ActiveWithLeewayVerifier(int leeway) {
		Preconditions.checkArgument(leeway >= 0, "The leeway must be a positive number");
		this.leeway = leeway;
	}

	@Override
	public boolean test(JsonWebToken t) throws VerificationException {
		if (isExpired(t)) {
			throw new TokenNotActiveException(t, "Token is not active");
		}

		return true;
	}

	@JsonIgnore
	private boolean isExpired(JsonWebToken t) {
		final Long exp = t.getExp();
		if (exp == null || exp == 0) return false;
		final int currentTime = Time.currentTime();
		return currentTime > (exp + leeway);
	}
}
