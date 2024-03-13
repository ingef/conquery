package com.bakdata.conquery.models.auth.web;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.ConqueryAuthenticator;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.google.common.base.Function;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * This filter hooks into dropwizard's request handling to extract and process
 * security relevant information for protected resources. The request is first
 * submitted to the registered {@link ConqueryAuthenticationRealm}s for the
 * token extraction, then the extracted tokens are submitted these realms
 * through Dropwizards {@link AuthFilter} and Shiro.
 */
@Slf4j
@PreMatching
@NoArgsConstructor
@Priority(Priorities.AUTHENTICATION)
@Provider
public class DefaultAuthFilter extends AuthFilter<AuthenticationToken, Subject> implements Feature {

	@Inject
	@Setter
	private IterableProvider<TokenExtractor> tokenExtractors;


	public static DefaultAuthFilter asDropwizardFeature() {
		final DefaultAuthFilter authFilter =
				new Builder()
						.setAuthenticator(new ConqueryAuthenticator())
						.setUnauthorizedHandler(new DefaultUnauthorizedHandler())
						.buildAuthFilter();
		return authFilter;
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
		log.warn("Non of the configured realms was able to successfully authenticate the extracted token(s).");
		log.trace("The {} tokens failed.", failedTokens);
		throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
	}

	public static void registerTokenExtractor(TokenExtractor extractor, ResourceConfig config) {
		config.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(extractor)
						.to(TokenExtractor.class);
			}
		});
	}

	@Override
	public boolean configure(FeatureContext context) {
		//TODO what does need to be initialized here?
		// NOTE that initialization fails, if we dont implement Feature for some reason.
		return true;
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
	 * token could be parsed.
	 */
	public interface TokenExtractor extends Function<ContainerRequestContext, AuthenticationToken> {

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
	private static class Builder extends AuthFilterBuilder<AuthenticationToken, Subject, DefaultAuthFilter> {

		@Override
		protected DefaultAuthFilter newInstance() {
			return new DefaultAuthFilter();
		}
	}
}
