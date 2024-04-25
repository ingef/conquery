package com.bakdata.conquery.models.auth.web.csrf;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Implementation of the Double-Submit-Cookie Pattern.
 * This filter generates a random token which is injected in to the response.
 * <ul>
 *     <li>In a Set-Cookie header, so that browser requests send the token via cookie back to us</li>
 *     <li>In the response payload. This filter sets a request property, which is eventually provided to freemarker.
 *     Freemarker then writes the token into payload (see base.html.ftl)</li>
 * </ul>
 */
public class CsrfTokenSetFilter implements ContainerRequestFilter, ContainerResponseFilter {

	public static final String CSRF_COOKIE_NAME = "csrf_token";
	public static final String CSRF_TOKEN_PROPERTY = "csrf_token";
	public static final int TOKEN_LENGTH = 30;

	Random random = new SecureRandom();

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		final String token = RandomStringUtils.random(TOKEN_LENGTH, 0, 0, true, true,
													  null, random
		);
		requestContext.setProperty(CSRF_TOKEN_PROPERTY, token);
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		final String csrfToken = getCsrfTokenProperty(requestContext);

		responseContext.getHeaders()
					   .add(HttpHeaders.SET_COOKIE, new NewCookie(CSRF_COOKIE_NAME, csrfToken, "/", null, 0, null, 3600, null, requestContext.getSecurityContext()
																																			 .isSecure(), false));
	}

	public static String getCsrfTokenProperty(ContainerRequestContext requestContext) {
		return (String) requestContext.getProperty(CSRF_TOKEN_PROPERTY);
	}
}
