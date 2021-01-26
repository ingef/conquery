package com.bakdata.conquery.models.auth.web;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.ConqueryAuthenticator;
import com.bakdata.conquery.models.auth.entities.User;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.Realm;

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
public class DefaultAuthFilter extends AuthFilter<AuthenticationToken, User> {

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
			throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
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
				if(tokens.size() > 1) {
					failedTokens++; 
					log.trace("Token authentication failed:",e);
					// If there is more than one token try the other ones too
					continue;
				}
				throw e;
			}
		}
		log.warn("Non of the configured realms was able to successfully authenticate the extracted token(s).");
		log.trace("The {} tokens failed.", failedTokens);
		throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
	}

	public void registerTokenExtractor(TokenExtractor extractor){
		tokenExtractors.add(extractor);
	}

	public static interface TokenExtractor extends Function<ContainerRequestContext, AuthenticationToken> {

	}

	/**
	 * Builder for {@link DefaultAuthFilter}.
	 * <p>
	 * An {@link AuthorizationController} must be provided during the building
	 * process.
	 * </p>
	 *
	 * @param <P>
	 *            the principal
	 */
	@Accessors(chain = true)
	@Setter
	private static class Builder extends AuthFilterBuilder<AuthenticationToken, User, DefaultAuthFilter> {

		@Override
		protected DefaultAuthFilter newInstance() {
			return new DefaultAuthFilter();
		}
	}

	public static DefaultAuthFilter asDropwizardFeature(MetaStorage storage) {
		Builder builder = new Builder();
		DefaultAuthFilter authFilter = builder
				.setAuthenticator(new ConqueryAuthenticator(storage)).setUnauthorizedHandler(new DefaultUnauthorizedHandler())
				.buildAuthFilter();
		return authFilter;
	}
}
