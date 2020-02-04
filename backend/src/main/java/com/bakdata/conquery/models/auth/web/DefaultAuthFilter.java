package com.bakdata.conquery.models.auth.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.NotAuthorizedException;
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
 * security relevant information for protected resources. Under the hood it sets
 * up shiro's security management, for the authentication of the requests. This
 * security management is then also used for authorizations based on
 * permissions, that the handling of a request triggers.
 */
@Slf4j
@PreMatching
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultAuthFilter extends AuthFilter<AuthenticationToken, User> {
	
	private final AuthorizationController controller;

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {

		
		// The token extraction process
		List<AuthenticationToken> tokens = new ArrayList<>();
		for(ConqueryAuthenticationRealm realm : controller.getAuthenticationRealms()) {
			AuthenticationToken token = null;
			if ((token = realm.extractToken(requestContext)) != null){
				log.trace("Realm {} extracted a token form the request: {}", ((Realm)realm).getName(), token);
				tokens.add(token);
			}
			log.trace("Realm {} did not extract a token form the request.", ((Realm)realm).getName());
		}
		
		Throwable exceptions = new Throwable("Authentication failed with");
		
		// The authentication process
		for(AuthenticationToken token :tokens) {
			try {
					// Submit the token to dropwizard which forwards it to Shiro
					if (!authenticate(requestContext, token, SecurityContext.BASIC_AUTH)) {
						throw new NotAuthorizedException("Authentication failed","Bearer");
					}
					// Success an extracted token could be authenticated
					log.trace("Authentication was successfull for token type {}", token.getClass().getName());
					return;
					
			}
			catch (Exception e) {
				exceptions.addSuppressed(e);
			}
		}
		log
		.warn(
			"Shiro failed to authenticate the request. See the following message by {}:\n\t{}",
			exceptions);
		throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
	}

	/**
	 * Builder for {@link DefaultAuthFilter}.
	 * <p>
	 * An {@link AuthorizationController} must be provided during the building process.
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

	public static  AuthFilter<AuthenticationToken, User> asDropwizardFeature(AuthorizationController controller) {
		Builder builder = new Builder();
		DefaultAuthFilter authFilter = builder
			.setController(controller)
			.setAuthenticator(new ConqueryAuthenticator(controller.getStorage()))
			.setUnauthorizedHandler(new DefaultUnauthorizedHandler())
			.buildAuthFilter();
		return authFilter;
	}
}
