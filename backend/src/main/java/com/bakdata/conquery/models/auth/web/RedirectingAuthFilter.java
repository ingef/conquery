package com.bakdata.conquery.models.auth.web;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.auth.AuthFilter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationToken;

import javax.annotation.Priority;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The {@link RedirectingAuthFilter} first delegates a request to the actual authentication filter.
 * If that filter is unable to map a user to the request, this filter checks if this request is in
 * the phase of a multi-step authentication, such as the OAuth-Code-Flow, or if a new login procedure
 * must be initiated.
 *
 * Depending on the configuration, none, one or multiple login schemas are available. If none is configured,
 * The filter responds with an {@link ServiceUnavailableException}. When one schema is configured, the user
 * is directly forwarded to that schema. If multiple schemas are possible the user is presented a webpage, where
 * the login schema can be chosen.
 */
@RequiredArgsConstructor
@Slf4j
@Priority(Priorities.AUTHENTICATION)
public class RedirectingAuthFilter extends AuthFilter<AuthenticationToken, User> {

	/**
	 * The Filter that checks if a request was authenticated
	 */
	private final DefaultAuthFilter delegate;

	/**
	 * Request processors that check if an request belongs to its multi-step authentication schema.
	 * E.g. the request contains an authorization code, then this checker tries to redeemed the code for an access token.
	 * If that succeeds, it produces a response that sets a cookie with the required authentication data for that schema.
	 *
	 * If the request does not fit the schema, the processor returns null.
	 */
	@Getter
	private final List<Function<ContainerRequestContext,Response>> authAttemptCheckers = new ArrayList<>();

	/**
	 * Request processors that produce a link to initiate a login procedure.
	 */
	@Getter
	private final List<Function<ContainerRequestContext,URI>> loginInitiators = new ArrayList<>();

	@Override
	public void filter(ContainerRequestContext request) throws IOException {
		try{
			delegate.filter(request);
		} catch (NotAuthorizedException e) {
			// The request could not be authenticated
			// First check if the request belongs to a multi-step authentication
			List<Response> authenticatedRedirects = new ArrayList<>();
			for ( Function<ContainerRequestContext,Response> authAttemptChecker : authAttemptCheckers) {
				Response response = authAttemptChecker.apply(request);
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


			List<URI> loginRedirects = new ArrayList<>();
			for ( Function<ContainerRequestContext,URI> loginInitiator : loginInitiators) {
				URI uri = loginInitiator.apply(request);
				if (uri != null) {
					loginRedirects.add(uri);
				}
			}


			if (loginRedirects.isEmpty()) {
				throw new ServiceUnavailableException("No login schema configured");
			}

			// Give the user a choice to choose between them. (If there is only one schema, still redirect the user there)
			// to prevent too many redirects if there was a problem wit the authentication
			throw new WebApplicationException(Response.ok(new UIView<>("logins.html.ftl", loginRedirects)).build());
		}
	}
}
