package com.bakdata.conquery.models.auth.web;

import java.io.IOException;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;

import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.ConqueryAuthenticator;
import com.bakdata.conquery.models.auth.entities.User;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
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

		
		AuthenticationToken token = null;
		for(ConqueryAuthenticationRealm realm : controller.getAuthenticationRealms()) {
			if ((token = realm.extractToken(requestContext)) != null){
				log.trace("Realm {} extracted a token form the request: {}", ((Realm)realm).getName(), token);
				break;
			}
			log.trace("Realm {} did not extract a token form the request.", ((Realm)realm).getName());
		}
		
		try {
			// sets the security context in the request AND does the authentication
			if (!authenticate(requestContext, token, SecurityContext.BASIC_AUTH)) {
				throw new NotAuthorizedException("Authentication failed","Bearer");
			}
		}
		catch (AuthenticationException e) {
			log
				.warn(
					"Shiro failed to authenticate the request. See the following message by {}:\n\t{}",
					e.getStackTrace()[0],
					e.getMessage());
			throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.");
		}
	}

	/**
	 * Builder for {@link DefaultAuthFilter}.
	 * <p>
	 * An {@link Authenticator} must be provided during the building process.
	 * </p>
	 *
	 * @param <P>
	 *            the principal
	 */
	@Setter()
	private static class Builder extends AuthFilterBuilder<AuthenticationToken, User, DefaultAuthFilter> {
		
		private AuthorizationController controller;

		@Override
		protected DefaultAuthFilter newInstance() {
			return new DefaultAuthFilter(controller);
		}
	}

	public static  AuthFilter<AuthenticationToken, User> asDropwizardFeature(AuthorizationController controller) {
		Builder builder = new Builder();
		DefaultAuthFilter authFilter = builder
			.setAuthenticator(new ConqueryAuthenticator(controller.getStorage()))
			.setUnauthorizedHandler(new DefaultUnauthorizedHandler())
			.buildAuthFilter();
		return authFilter;
	}
}
