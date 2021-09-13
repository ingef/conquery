package com.bakdata.conquery.models.auth.web;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.ConqueryAuthenticator;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.entities.Userish;
import com.google.common.base.Function;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;

import javax.annotation.Priority;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * This filter hooks into dropwizard's request handling to extract and process
 * security relevant information for protected resources. The request is first
 * submitted to the registered {@link ConqueryAuthenticationRealm}s for the
 * token extraction, then the extracted tokens are submitted these realms
 * through Dropwizards {@link AuthFilter} and Shiro.
 */
@Slf4j
@PreMatching
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Priority(Priorities.AUTHENTICATION)
public class DefaultAuthFilter extends AuthFilter<AuthenticationToken, Userish> {

	private final Set<TokenExtractor> tokenExtractors = new HashSet<>();

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {

		// The token extraction process
		Set<AuthenticationToken> tokens = new HashSet<>();
		for (TokenExtractor tokenExtractor : tokenExtractors) {
			AuthenticationToken token = null;
			if ((token = tokenExtractor.apply(requestContext) ) != null) {
				log.trace("Extracted a token form the request: {}", token);
				tokens.add(token);
			}
		}

		if (tokens.isEmpty()) {
			log.warn("No tokens could be parsed from the request");
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
				log.trace("Authentication was successfull for token type {}", token.getClass().getName());
				return;
			} catch (AuthenticationException e) {
				// This is the shiro way to indicate that authentication failed
				failedTokens++;
				log.trace("Token authentication failed:",e);
				// If there is more than one token try the other ones too

			}
		}
		log.warn("Non of the configured realms was able to successfully authenticate the extracted token(s).");
		log.trace("The {} tokens failed.", failedTokens);
		throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
	}

	public void registerTokenExtractor(TokenExtractor extractor){
		if(!tokenExtractors.add(extractor)) {
			log.info("Token extractor {} was already added.", extractor.getClass().getName());
		}
	}

	/**
	 * Authenticating realms need to be able to extract a token from a request. How
	 * it performs the extraction is implementation dependent. Anyway the realm
	 * should NOT alter the request. This function is called prior to the
	 * authentication process in the {@link DefaultAuthFilter}. After the token
	 * extraction process the Token is resubmitted to the realm from the AuthFilter
	 * to the {@link ConqueryAuthenticator} which dispatches it to shiro.
	 *
	 * @return The extracted {@link AuthenticationToken} or <code>null</code> if no
	 *         token could be parsed.
	 */
	public static interface TokenExtractor extends Function<ContainerRequestContext, AuthenticationToken> {

	}

	/**
	 * Builder for {@link DefaultAuthFilter}.
	 * <p>
	 * An {@link AuthorizationController} must be provided during the building
	 * process.
	 * </p>
	 */
	@Accessors(chain = true)
	@Setter
	private static class Builder extends AuthFilterBuilder<AuthenticationToken, Userish, DefaultAuthFilter> {

		@Override
		protected DefaultAuthFilter newInstance() {
			return new DefaultAuthFilter();
		}
	}

	public static DefaultAuthFilter asDropwizardFeature(MetaStorage storage) {
		Builder builder = new Builder();
		DefaultAuthFilter authFilter = builder
				.setAuthenticator(new ConqueryAuthenticator()).setUnauthorizedHandler(new DefaultUnauthorizedHandler())
				.buildAuthFilter();
		return authFilter;
	}
}
