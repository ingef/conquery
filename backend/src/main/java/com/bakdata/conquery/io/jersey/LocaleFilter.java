package com.bakdata.conquery.io.jersey;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import com.bakdata.conquery.models.i18n.I18n;


public class LocaleFilter implements ContainerRequestFilter {
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		I18n.LOCALE.set(requestContext.getLanguage());
	}

}
