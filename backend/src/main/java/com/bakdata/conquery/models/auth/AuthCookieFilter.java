package com.bakdata.conquery.models.auth;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.Provider;

import org.eclipse.jetty.http.HttpHeader;

import com.bakdata.conquery.io.jersey.AuthCookie;

@Provider
@AuthCookie
public class AuthCookieFilter implements ContainerResponseFilter {
	
	private static final String ACCESS_TOKEN = "access_token";

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
		Cookie cookie = request.getCookies().get(ACCESS_TOKEN);
		
		if(cookie == null)		
			response.getHeaders().add(
					HttpHeader.SET_COOKIE.toString(),
					new NewCookie(ACCESS_TOKEN, request.getUriInfo().getQueryParameters().getFirst(ACCESS_TOKEN))
			);
	}
}
