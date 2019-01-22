package com.bakdata.conquery.models.auth;

import java.io.IOException;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.realm.AuthorizingRealm;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.User;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This filter hooks into dropwizard's request handling to extract and process
 * security relevant information for protected resources. Under the hood it sets
 * up shiro's security management, for the authentication of the requests. This
 * security management is then also used for authorizations based on
 * permissions, that the handling of a request triggers.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultAuthFilter extends AuthFilter<ConqueryToken, User> {

	private final TokenExtractor tokenExtractor;

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {

		ConqueryToken credentials = tokenExtractor.extract(requestContext);

		try {
			// sets the security context in the request AND does the authentication
			if (!authenticate(requestContext, credentials, SecurityContext.BASIC_AUTH)) {
				throw new NotAuthorizedException("Failed to authenticate request");
			}
		}
		catch (AuthenticationException e) {
			log.warn("Shiro failed to authenticate the request. See the following trace:", e);
			throw new NotAuthorizedException("Failed to authenticate request. The cause has been logged.", e);
		}
	}

	private static class DefaultAuthFilterBuilder extends AuthFilterBuilder<ConqueryToken, User, DefaultAuthFilter> {

		private TokenExtractor tokenExtractor = new DefaultTokenExtractor();

		public DefaultAuthFilterBuilder setTokenExtractor(TokenExtractor tokenExtractor) {
			this.tokenExtractor = tokenExtractor;
			return this;
		}

		@Override
		protected DefaultAuthFilter newInstance() {
			return new DefaultAuthFilter(tokenExtractor);
		}
	}

	public static DefaultAuthFilterBuilder builder() {
		return new DefaultAuthFilterBuilder();
	}

	public static AuthDynamicFeature asDropwizardFeature(MasterMetaStorage storage, AuthConfig config) {
		AuthorizingRealm realm = config.getRealm(storage);
		UnknownUserHandler uuHandler = config.getUnknownUserHandler(storage);

		DefaultAuthFilterBuilder builder = DefaultAuthFilter.builder();
		AuthFilter<ConqueryToken, User> authFilter = builder
			.setTokenExtractor(config.getTokenExtractor())
			.setAuthenticator(new ConqueryAuthenticator(storage, realm, uuHandler))
			.setUnauthorizedHandler(new DefaultUnauthorizedHandler())
			.buildAuthFilter();
		return new AuthDynamicFeature(authFilter);
	}
}
