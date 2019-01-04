package com.bakdata.conquery.external.auth.ingef;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.ConqueryToken;
import com.bakdata.conquery.models.auth.TokenExtractor;

import lombok.RequiredArgsConstructor;

@CPSType(id="INGEF_TOKEN_PARSER", base=TokenExtractor.class)
@RequiredArgsConstructor
public class IngefTokenExtractor implements TokenExtractor{
	public static final String OAUTH_ACCESS_TOKEN_PARAM = "access_token";
	
	private final String prefix = "Bearer";
	
	public ConqueryToken extract(ContainerRequestContext requestContext) {
		String credentials = getCredentials(requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
		
		// If Authorization header is not used, check query parameter where token can be passed as well
		if (credentials == null) {
			credentials = requestContext.getUriInfo().getQueryParameters().getFirst(OAUTH_ACCESS_TOKEN_PARAM);
		}
		return new ConqueryToken(credentials);
	}

	
	/**
	 * Parses a value of the `Authorization` header in the form of `Bearer a892bf3e284da9bb40648ab10`.
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
		if (!prefix.equalsIgnoreCase(method)) {
			return null;
		}

		return header.substring(space + 1);
	}
}
