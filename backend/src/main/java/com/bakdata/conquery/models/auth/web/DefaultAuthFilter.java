package com.bakdata.conquery.models.auth.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;

import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.ConqueryAuthenticator;
import com.bakdata.conquery.models.auth.entities.User;
import com.google.common.base.Preconditions;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
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

	private final AuthorizationController controller;

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {

		// The token extraction process
		List<AuthenticationToken> tokens = new ArrayList<>();
		for (ConqueryAuthenticationRealm realm : controller.getAuthenticationRealms()) {
			AuthenticationToken token = null;
			if ((token = realm.extractToken(requestContext)) != null) {
				log.trace("Realm {} extracted a token form the request: {}", ((Realm) realm).getName(), token);
				tokens.add(token);
			} else {				
				log.trace("Realm {} did not extract a token form the request.", ((Realm) realm).getName());
			}
		}

		if (tokens.isEmpty()) {
			log.warn("No tokens could be parsed from the request");
			throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
		}

		List<AuthenticationToken> failedTokens = new ArrayList<>();

		// The authentication process
		for (AuthenticationToken token : tokens) {
			// Submit the token to dropwizard which forwards it to Shiro
			if (!authenticate(requestContext, token, SecurityContext.BASIC_AUTH)) {
				failedTokens.add(token);
				continue;
			}
			// Success an extracted token could be authenticated
			log.trace("Authentication was successfull for token type {}", token.getClass().getName());
			return;
		}
		log.warn("Non of the configured realms was able to successfully authenticate the following token(s).");
		log.trace("The failing tokens were: {}", failedTokens);
		throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
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

		private AuthorizationController controller;

		@Override
		protected DefaultAuthFilter newInstance() {
			Preconditions.checkNotNull(controller);
			return new DefaultAuthFilter(controller);
		}
	}

	public static AuthFilter<AuthenticationToken, User> asDropwizardFeature(AuthorizationController controller) {
		Builder builder = new Builder();
		DefaultAuthFilter authFilter = builder.setController(controller)
			.setAuthenticator(new ConqueryAuthenticator(controller.getStorage())).setUnauthorizedHandler(new DefaultUnauthorizedHandler())
			.buildAuthFilter();
		return authFilter;
	}
}
