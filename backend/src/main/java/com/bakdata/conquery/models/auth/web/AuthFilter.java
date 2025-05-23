package com.bakdata.conquery.models.auth.web;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.ConqueryAuthenticator;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.google.common.base.Function;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.Contract;

/**
 * This filter hooks into dropwizard's request handling to extract and process
 * security relevant information for protected resources. The request is first
 * submitted to the registered {@link ConqueryAuthenticationRealm}s for the
 * token extraction, then the extracted tokens are submitted these realms
 * through Dropwizards {@link io.dropwizard.auth.AuthFilter} and Shiro.
 */
@Slf4j
@PreMatching
@Priority(AuthFilter.PRIORITY)
public class AuthFilter extends io.dropwizard.auth.AuthFilter<AuthenticationToken, Subject> {

	public static final int PRIORITY = Priorities.AUTHENTICATION;

	private final IterableProvider<TokenExtractor> tokenExtractors;

	@Inject
	public AuthFilter(IterableProvider<TokenExtractor> tokenExtractors) {
		this.tokenExtractors = tokenExtractors;
		this.authenticator = new ConqueryAuthenticator();
		this.unauthorizedHandler = new DefaultUnauthorizedHandler();
	}

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {

		// The token extraction process
		final Set<AuthenticationToken> tokens = new HashSet<>();
		for (final TokenExtractor tokenExtractor : tokenExtractors) {
			AuthenticationToken token = null;
			if ((token = tokenExtractor.apply(requestContext)) != null) {
				log.trace("Extracted a token form the request: {}", token);
				tokens.add(token);
			}
		}

		if (tokens.isEmpty()) {
			log.trace("No tokens could be parsed from the request");
		}

		int failedTokens = 0;

		// The authentication process
		for (AuthenticationToken token : tokens) {
			try {
				// Submit the token to dropwizard which forwards it to Shiro
				if (!authenticate(requestContext, token, SecurityContext.BASIC_AUTH)) {
					// This is the dropwizard way to indicate that authentication failed
					failedTokens++;
					// Continue with next token
					continue;
				}
				// Success an extracted token could be authenticated
				log.trace("Authentication was successful for token type {}", token.getClass().getName());
				return;
			}
			catch (AuthenticationException e) {
				// This is the shiro way to indicate that authentication failed
				failedTokens++;
				log.trace("Token authentication failed:", e);
				// If there is more than one token try the other ones too

			}
		}
		log.trace("Non of the configured realms was able to successfully authenticate the extracted token(s).");
		log.trace("The {} tokens failed.", failedTokens);
		throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
	}

	public static void registerTokenExtractor(Class<? extends TokenExtractor> extractor, ResourceConfig config) {
		config.register(extractor);
	}

	/**
	 * Authenticating realms need to be able to extract a token from a request. How
	 * it performs the extraction is implementation dependent. Anyway the realm
	 * should NOT alter the request. This function is called prior to the
	 * authentication process in the {@link AuthFilter}. After the token
	 * extraction process the Token is resubmitted to the realm from the AuthFilter
	 * to the {@link ConqueryAuthenticator} which dispatches it to shiro.
	 *
	 * @return The extracted {@link AuthenticationToken} or <code>null</code> if no
	 * token could be parsed.
	 */
	@Contract
	public interface TokenExtractor extends Function<ContainerRequestContext, AuthenticationToken> {

	}
}
