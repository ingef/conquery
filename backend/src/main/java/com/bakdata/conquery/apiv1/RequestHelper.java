package com.bakdata.conquery.apiv1;

import java.net.URL;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestHelper {

	public static String getHeader(ContainerRequestContext requestContext,
			String key, String defaultValue) {
		String header = requestContext.getHeaders().getFirst(key);
		return Optional.ofNullable(header)
				.orElse(defaultValue);
	}

	public static String getUserAgent(ContainerRequestContext requestContext,
			String defaultValue) {
		return getHeader(requestContext, HttpHeaders.USER_AGENT, defaultValue);
	}

	public static String getIp(HttpServletRequest request, String defaultValue) {
		return Optional.ofNullable(request)
				.map(r
						-> Optional.ofNullable(r.getHeader(AdditionalHeaders.HTTP_HEADER_REAL_IP))
						.orElse(r.getRemoteAddr())
				)
				.orElse(defaultValue);
	}

	public static String getRequestURL(HttpServletRequest req) {
		if (req.getHeader(AdditionalHeaders.HTTP_HEADER_REAL_HOST) != null) {
			try {
				return new URL(
						req.getHeader(AdditionalHeaders.HTTP_HEADER_REAL_PROTO),
						req.getHeader(AdditionalHeaders.HTTP_HEADER_REAL_HOST),
						""
				).toString();
			} catch (Exception e) {
				log.warn("Failed to build response URL from X-Forward headers", e);
			}
		}

		String host = req.getRequestURL().toString();
		host = StringUtils.removeEnd(host, req.getPathInfo()); //remove path of called method
		return StringUtils.removeEnd(host, req.getServletPath()); //remove prefix path
	}

	public static Optional<Principal> getPrincipal(ContainerRequestContext requestContext) {
		return getSecurityContext(requestContext)
				.map(SecurityContext::getUserPrincipal);
	}

	public static Optional<SecurityContext> getSecurityContext(ContainerRequestContext requestContext) {
		return Optional.ofNullable(requestContext.getSecurityContext());
	}
}
