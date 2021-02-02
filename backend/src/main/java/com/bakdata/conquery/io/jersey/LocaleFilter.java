package com.bakdata.conquery.io.jersey;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;

import com.bakdata.conquery.models.i18n.I18n;

/**
 * Extracts the user specific locale for internationalization, that is contained
 * in the request header. If none is provided, it falls back to
 * {@link I18n#DEFAULT_LOCALE}.
 */

public class LocaleFilter implements ContainerRequestFilter {
	
	@Context
	private HttpServletRequest request;
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// Use jetty context here because requestContext.getLanguage() does not work
		Locale locale = request.getLocale();
		if (locale == null) {
			locale = Locale.ROOT;
		}
		I18n.LOCALE.set(locale);
	}

}
