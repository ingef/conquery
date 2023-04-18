package com.bakdata.conquery.apiv1;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.http.HttpHeader;

@Slf4j
@UtilityClass
public class RequestHelper {

	public static String getRequestURL(HttpServletRequest req) {
		if (req.getHeader(HttpHeader.X_FORWARDED_HOST.asString()) != null) {
			try {
				final String host = req.getHeader(HttpHeader.X_FORWARDED_HOST.asString());
				final String protocol = req.getHeader(HttpHeader.X_FORWARDED_PROTO.asString());

				log.trace("Proto=`{}` Fwd-Host=`{}`", protocol, host);

				return new URIBuilder()
						.setHost(host)
						.setScheme(protocol)
						.toString();
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
		if (headers.getFirst(HttpHeader.X_FORWARDED_HOST.asString()) != null) {
			try {

				final String host = headers.getFirst(HttpHeader.X_FORWARDED_HOST.asString());
				final String protocol = headers.getFirst(HttpHeader.X_FORWARDED_PROTO.asString());

				log.trace("Proto=`{}` Fwd-Host=`{}`", protocol, host);

				return new URIBuilder()
						.setHost(host)
						.setScheme(protocol)
						.build();
			}
			catch (Exception e) {
				log.warn("Failed to build response URL from X-Forward headers", e);
			}
		}

		// Fallback: drop path and query, use only schema, authority and port
		return req.getUriInfo().getRequestUriBuilder().replacePath(null).replaceQuery(null).build();
	}

}
