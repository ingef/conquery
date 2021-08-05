package com.bakdata.conquery.models.auth.web;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.auth.AuthFilter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationToken;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
public class RedirectingAuthFilter extends AuthFilter<AuthenticationToken, User> {

	private final DefaultAuthFilter delegate;
	@Getter
	private final List<Function<ContainerRequestContext,Response>> authAttemptCheckers = new ArrayList<>();
	@Getter
	private final List<Function<ContainerRequestContext,URI>> loginInitiators = new ArrayList<>();

	@Override
	public void filter(ContainerRequestContext request) throws IOException {
		try{
			delegate.filter(request);
		} catch (NotAuthorizedException e) {
			List<Response> authenticatedRedirects = new ArrayList<>();
			for ( Function<ContainerRequestContext,Response> authAttemptChecker : authAttemptCheckers) {
				Response response = authAttemptChecker.apply(request);
				if (response != null) {
					authenticatedRedirects.add(response);
				}
			}

			if (authenticatedRedirects.size() == 1) {
				throw new RedirectionException(authenticatedRedirects.get(0));
			}
			else if (authenticatedRedirects.size() > 1) {
				log.error("Multiple authenticated redirects generated. Only one should be possible");
			}

			log.info("Redirecting unauthenticated user to login schema");


			List<URI> loginRedirects = new ArrayList<>();
			for ( Function<ContainerRequestContext,URI> loginInitiator : loginInitiators) {
				URI uri = loginInitiator.apply(request);
				if (uri != null) {
					loginRedirects.add(uri);
				}
			}


			if (loginRedirects.size() == 0) {
				throw new ServiceUnavailableException("No Login schema configured");
			}
			else if (loginRedirects.size() == 1) {
				throw new WebApplicationException(Response.seeOther(loginRedirects.get(0)).build());
			}
			throw new WebApplicationException(Response.ok(new UIView<>("logins.html.ftl", null, loginRedirects)).build());
		}
	}
}
