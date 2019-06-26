package com.bakdata.eva.models.auth;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

import com.bakdata.conquery.models.auth.ConqueryToken;
import com.bakdata.conquery.models.auth.TokenExtractor;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IngefTokenExtractor implements TokenExtractor {
	public static final String OAUTH_ACCESS_TOKEN_PARAM = "access_token";

	private static final String PREFIX = "Bearer";

	@Override
	public ConqueryToken extract(ContainerRequestContext requestContext) {
		String credentials = getCredentials(requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

		// If Authorization header is not used, check query parameter where token can be
		// passed as well
		if (credentials == null)
			credentials = requestContext.getUriInfo().getQueryParameters().getFirst(OAUTH_ACCESS_TOKEN_PARAM);

		// This Cookie is only set in HDatasets
		Cookie cookie = requestContext.getCookies().get(OAUTH_ACCESS_TOKEN_PARAM);
		if (cookie != null && credentials == null)
			credentials = cookie.getValue();

		return new ConqueryToken(credentials);
	}

	/**
	 * Parses a value of the `Authorization` header in the form of `Bearer
	 * a892bf3e284da9bb40648ab10`.
	 *
	 * @param header the value of the `Authorization` header
	 * @param prefix prefix
	 * @return a token
	 */
	@Nullable
	private String getCredentials(String header) {
		if (header == null) {
			return null;
		}

		final int space = header.indexOf(' ');
		if (space <= 0) {
			return null;
		}

		final String method = header.substring(0, space);
		if (!PREFIX.equalsIgnoreCase(method)) {
			return null;
		}

		return header.substring(space + 1);
	}
}
