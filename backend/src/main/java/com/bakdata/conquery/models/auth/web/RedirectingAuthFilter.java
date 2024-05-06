package com.bakdata.conquery.models.auth.web;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RedirectionException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationToken;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.Contract;

/**
 * The {@link RedirectingAuthFilter} first delegates a request to the actual authentication filter.
 * If that filter is unable to map a user to the request, this filter checks if this request is in
 * the phase of a multi-step authentication, such as the OAuth-Code-Flow, or if a new login procedure
 * must be initiated.
 * <p>
 * Depending on the configuration, none, one or multiple login schemas are available. If none is configured,
 * The filter responds with an {@link ServiceUnavailableException}. When one schema is configured, the user
 * is directly forwarded to that schema. If multiple schemas are possible the user is presented a webpage, where
 * the login schema can be chosen.
 */
@Slf4j
@Priority(RedirectingAuthFilter.PRIORITY)
@PreMatching
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RedirectingAuthFilter extends io.dropwizard.auth.AuthFilter<AuthenticationToken, User> {

	public static final int PRIORITY = Priorities.AUTHENTICATION;

	public static final String REDIRECT_URI = "redirect_uri";
	/**
	 * Request processors that check if a request belongs to its multi-step authentication schema.
	 * E.g. the request contains an authorization code, then this checker tries to redeem the code for an access token.
	 * If that succeeds, it produces a response that sets a cookie with the required authentication data for that schema.
	 * <p>
	 * If the request does not fit the schema, the processor returns null.
	 */
	private final IterableProvider<AuthAttemptChecker> authAttemptCheckers;
	/**
	 * Request processors that produce a link to initiate a login procedure.
	 */
	private final IterableProvider<LoginInitiator> loginInitiators;
	/**
	 * The Filter that checks if a request was authenticated
	 */
	private final AuthFilter delegate;

	public static void registerLoginInitiator(ResourceConfig resourceConfig, LoginInitiator initiator, final String name) {
		resourceConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(initiator)
						.named(name)
						.to(LoginInitiator.class);
			}
		});
	}

	public static void registerAuthAttemptChecker(ResourceConfig resourceConfig, AuthAttemptChecker checker, final String name) {
		//TODO These bindings dont work yet, we need to use concrete classes instead of lambdas
		resourceConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(checker)
						.named(name)
						.to(AuthAttemptChecker.class);
			}
		});
	}

	@Override
	public void filter(ContainerRequestContext request) throws IOException {
		try {
			delegate.filter(request);
		}
		//TODO shouldn't this be something with NotAuthenticated?
		catch (NotAuthorizedException e) {
			// The request could not be authenticated
			// First check if the request belongs to a multi-step authentication
			final List<Response> authenticatedRedirects = new ArrayList<>();

			for (Function<ContainerRequestContext, Response> authAttemptChecker : authAttemptCheckers) {

				final Response response = authAttemptChecker.apply(request);

				if (response != null) {
					authenticatedRedirects.add(response);
				}
			}

			if (authenticatedRedirects.size() == 1) {
				// The request qualified as a multi-step authentication, so the user is redirected to proceed the authentication.
				throw new RedirectionException(authenticatedRedirects.get(0));
			}
			else if (authenticatedRedirects.size() > 1) {
				log.error("Multiple authenticated redirects generated. Only one should be possible");
				throw new BadRequestException("The request triggered more than one multi-step authentication schema, which is not allowed.");
			}

			// The request was not authenticated, nor was it a step towards an authentication, so we redirect the user to a login.

			log.info("Redirecting unauthenticated user to login schema");
			final List<URI> loginRedirects = new ArrayList<>();

			for (Function<ContainerRequestContext, URI> loginInitiator : loginInitiators) {

				final URI uri = loginInitiator.apply(request);

				if (uri != null) {
					loginRedirects.add(uri);
				}
			}


			if (loginRedirects.isEmpty()) {
				throw new ServiceUnavailableException("No login schema configured");
			}

			// shortcut when only one login provider is configured
			if (loginRedirects.size() == 1) {
				final URI loginUri = loginRedirects.get(0);
				log.trace("One login redirect configured. Short cutting to: {}", loginUri);
				throw new WebApplicationException(Response.seeOther(loginUri).build());
			}

			// Give the user a choice to choose between them.
			throw new WebApplicationException(Response.ok(new UIView<>("logins.html.ftl", null, loginRedirects)).build());
		}
	}

	@Contract
	public interface LoginInitiator extends Function<ContainerRequestContext, URI> {
	}

	@Contract
	public interface AuthAttemptChecker extends Function<ContainerRequestContext, Response> {
	}
}
