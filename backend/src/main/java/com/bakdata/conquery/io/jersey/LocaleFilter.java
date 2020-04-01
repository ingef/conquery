package com.bakdata.conquery.io.jersey;

import java.io.IOException;
import java.util.Locale;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import com.bakdata.conquery.models.i18n.I18n;

/**
 * Extracts the user specific locale for internationalization, that is contained
 * in the request header. If none is provided, it falls back to
 * {@link I18n#DEFAULT_LOCALE}.
 */
public class LocaleFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Locale locale = requestContext.getLanguage();
		if (locale == null) {
			locale = I18n.DEFAULT_LOCALE;
		}
		I18n.LOCALE.set(locale);
	}

}
