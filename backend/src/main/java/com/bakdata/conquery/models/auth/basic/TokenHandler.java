package com.bakdata.conquery.models.auth.basic;

import java.util.Date;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.authc.AuthenticationToken;

@UtilityClass
@Slf4j
public class TokenHandler {
	private static final String PREFIX =  "Bearer";
	private static final String OAUTH_ACCESS_TOKEN_PARAM = "access_token";
	// Pattern from https://www.regextester.com/105777
	private static final String JWT_PATTERN = "^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$";

	/**
	 * Creates a signed JWT token for the authentication with the {@link LocalAuthenticationRealm}.
	 * @param username
	 * @param expiration
	 * @param issuer
	 * @param algorithm
	 * @return
	 */
	public String createToken(String username, int expiration, String issuer, Algorithm algorithm) {
		Date issueDate = new Date();
		Date expDate = DateUtils.addHours(issueDate, expiration);
		String token = JWT.create()
			.withIssuer(issuer)
			.withSubject(username)
			.withIssuedAt(issueDate)
			.withExpiresAt(expDate)
			.sign(algorithm);
		return token;
	}
	
	@Nullable
	public static AuthenticationToken extractToken(ContainerRequestContext request) {
		String token = null;
		String tokenHeader = extractTokenFromHeader(request);
		String tokenQuery = extractTokenFromQuery(request);
		if(tokenHeader == null && tokenQuery == null) {
			// No token could be parsed
			return null;
		} else if (tokenHeader != null && tokenQuery != null) {
			log.warn("There were tokens in the request header and query string provided, which is forbidden. See: https://tools.ietf.org/html/rfc6750#section-2");
			return null;
		} else if (tokenHeader != null) {
			log.trace("Extraced the request header token");
			token = tokenHeader;
		} else {
			log.trace("Extraced the query string token");
			token = tokenQuery;
		}
		
		if(token.matches(JWT_PATTERN)) {			
			return new JWTToken(token);
		}
		return null;		
	}
	
	/**
	 * Code obtained from the Dropwizard project {@link OAuthCredentialAuthFilter}.
	 * 
	 * Parses a value of the `Authorization` header in the form of `Bearer a892bf3e284da9bb40648ab10`.
	 *
	 * @param header the value of the `Authorization` header
	 * @return a token
	 */
	private static String extractTokenFromHeader(ContainerRequestContext request) {

        final String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
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
	
	@Nullable
	private static String extractTokenFromQuery(ContainerRequestContext request) {
		// If Authorization header is not used, check query parameter where token can be
		// passed as well		
		String credentials = request.getUriInfo().getQueryParameters().getFirst(OAUTH_ACCESS_TOKEN_PARAM);
		if(credentials != null) {
			return credentials;
		}
		return null;
	}

	@SuppressWarnings("serial")
	@AllArgsConstructor
	public static class JWTToken implements AuthenticationToken{
		private String token;

		@Override
		public Object getPrincipal() {
			throw new UnsupportedOperationException("No principal availibale for this token type");
		}

		@Override
		public Object getCredentials() {
			return token;
		}
	}
}
