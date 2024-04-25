package com.bakdata.conquery.models.auth.web.csrf;

import java.io.IOException;
import java.util.Optional;

import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of the Double-Submit-Cookie Pattern.
 * Checks if tokens in cookie and header match if a cookie is present.
 * Otherwise the request is refused.
 */
@Priority(AuthCookieFilter.PRIORITY - 100)
@Slf4j
public class CsrfTokenCheckFilter implements ContainerRequestFilter {
	public static final String CSRF_TOKEN_HEADER = "X-Csrf-Token";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		final String cookieToken = Optional.ofNullable(requestContext.getCookies().get(CsrfTokenSetFilter.CSRF_COOKIE_NAME)).map(Cookie::getValue).orElse(null);
		final String headerToken = requestContext.getHeaders().getFirst(CSRF_TOKEN_HEADER);

		if (cookieToken == null) {
			log.trace("Request had no csrf token set. Accepting request");
			return;
		}

		if (StringUtils.isBlank(headerToken)) {
			log.warn("Request contained csrf cookie but the header token was empty");
			throw new ForbiddenException("CSRF Attempt");
		}

		if (!cookieToken.equals(headerToken)) {
			log.warn("Request csrf cookie and header did not match");
			throw new ForbiddenException("CSRF Attempt");
		}

		log.trace("Csrf check successful");

	}
}
