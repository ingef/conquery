package com.bakdata.conquery.apiv1;

import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;

@Slf4j
@UtilityClass
public class RequestHelper {

	public static String getRequestURL(HttpServletRequest req) {
		if (req.getHeader(HttpHeader.X_FORWARDED_HOST.asString()) != null) {
			try {
				final String host = req.getHeader(HttpHeader.X_FORWARDED_HOST.asString());
				final String protocol = req.getHeader(HttpHeader.X_FORWARDED_PROTO.asString());

				log.debug("Proto=`{}` Fwd-Host=`{}`", protocol, host);

				return new URL(protocol, host, "").toString();
			}
			catch (Exception e) {
				log.warn("Failed to build response URL from X-Forward headers", e);
			}
		}

		String host = req.getRequestURL().toString();
		host = StringUtils.removeEnd(host, req.getPathInfo()); //remove path of called method
		return StringUtils.removeEnd(host, req.getServletPath()); //remove prefix path
	}

	/**
	 * Resolves proxied paths to the requested original URI if necessary.
	 */
	public static URI getRequestURL(ContainerRequestContext req) {
		final MultivaluedMap<String, String> headers = req.getHeaders();
		if (headers.getFirst(AdditionalHeaders.HTTP_HEADER_REAL_HOST) != null) {
			try {
				return new URL(
						headers.getFirst(AdditionalHeaders.HTTP_HEADER_REAL_PROTO),
						headers.getFirst(AdditionalHeaders.HTTP_HEADER_REAL_HOST),
						""
				).toURI();
			}
			catch (Exception e) {
				log.warn("Failed to build response URL from X-Forward headers", e);
			}
		}

		// Fallback: drop path and query, use only schema, authority and port
		return req.getUriInfo().getRequestUriBuilder().replacePath(null).replaceQuery(null).build();
	}

}
