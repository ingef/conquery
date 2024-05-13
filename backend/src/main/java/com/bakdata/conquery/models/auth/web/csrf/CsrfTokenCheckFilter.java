package com.bakdata.conquery.models.auth.web.csrf;

import java.io.IOException;
import java.util.Optional;

import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of the Double-Submit-Cookie Pattern.
 * Checks if tokens in cookie and header match if a cookie is present.
 * Otherwise, the request is refused.
 */
@Priority(AuthCookieFilter.PRIORITY - 100)
@Slf4j
public class CsrfTokenCheckFilter implements ContainerRequestFilter {
	public static final String CSRF_TOKEN_HEADER = "X-Csrf-Token";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		final String
				cookieTokenHash =
				Optional.ofNullable(requestContext.getCookies().get(CsrfTokenSetFilter.CSRF_COOKIE_NAME)).map(Cookie::getValue).orElse(null);
		final String headerToken = requestContext.getHeaders().getFirst(CSRF_TOKEN_HEADER);

		final String method = requestContext.getMethod();

		if (HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method) || HttpMethod.OPTIONS.equals(method)) {
			log.trace("Skipping csrf check because request is not state changing (method={})", method);
			return;
		}

		if (cookieTokenHash == null) {
			log.trace("Request had no csrf token set. Accepting request");
			return;
		}

		if (StringUtils.isBlank(headerToken)) {
			log.warn("Request contained csrf cookie but the header token was empty");
			throw new ForbiddenException("CSRF Attempt");
		}

		if (!CsrfTokenSetFilter.checkHash(headerToken, cookieTokenHash)) {
			log.warn("Request csrf cookie and header did not match");
			log.trace("header-token={} cookie-token-hash={}", headerToken, cookieTokenHash);
			throw new ForbiddenException("CSRF Attempt");
		}

		log.trace("Csrf check successful");
	}
}
