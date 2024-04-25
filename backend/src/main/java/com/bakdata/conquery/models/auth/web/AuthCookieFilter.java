package com.bakdata.conquery.models.auth.web;

import static com.bakdata.conquery.models.auth.web.AuthCookieFilter.PRIORITY;

import java.io.IOException;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.google.common.base.Strings;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.UriBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpHeader;

/**
 * This filter is only used for the admin endpoints which serve static sites
 * with links to these endpoints. Since it is not possible to set the
 * authorization header from static sites and adding the token to all links and
 * fetches on these sites is tedious, we use a cookie that is automatically send from the
 * browser with every request.
 *
 */
@Slf4j
@PreMatching
// Chain this filter before the Authentication filter
@Priority(PRIORITY)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AuthCookieFilter implements ContainerRequestFilter, ContainerResponseFilter {

	public static final int PRIORITY = Priorities.AUTHENTICATION - 100;

	public static final String ACCESS_TOKEN = "access_token";
	private static final String PREFIX = "bearer";

	private final ConqueryConfig config;

	/**
	 * The filter tries to extract a token from a cookie and puts it into the
	 * authorization header of the request. This simplifies the token retrival
	 * process for the realms.
	 */
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Cookie cookie = requestContext.getCookies().get(ACCESS_TOKEN);
		if (cookie == null) {
			return;
		}

		String queryToken = requestContext.getUriInfo().getQueryParameters().getFirst(ACCESS_TOKEN);

		if(!cookie.getValue().isEmpty() && queryToken != null) {
			log.trace("Ignoring cookie");
			return;
		}
		
		// Get the token from the cookie and put it into the header
		requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION,PREFIX + " " + cookie.getValue());
		// Remove the cookie for the rest of this processing
		requestContext.getCookies().remove(ACCESS_TOKEN);
		// Remove the query parameter
		UriBuilder uriBuilder = requestContext.getUriInfo().getRequestUriBuilder().replaceQueryParam(ACCESS_TOKEN);
		requestContext.setRequestUri(uriBuilder.build());

	}

	/**
	 * Sets a cookie with the access_token from the requests query string.
	 */
	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
		Cookie cookie = request.getCookies().get(ACCESS_TOKEN);
		String token = request.getUriInfo().getQueryParameters().getFirst(ACCESS_TOKEN);

		// Set cookie only if a token is present
		if (!Strings.isNullOrEmpty(token)) {
			if (cookie != null) {
				log.debug("Overwriting {} cookie", ACCESS_TOKEN);
			}
			final NewCookie authCookie = config.getAuthentication().createAuthCookie(request, token);
			response.getHeaders().add(
					HttpHeader.SET_COOKIE.toString(),
					authCookie
			);

		}
	}

}
