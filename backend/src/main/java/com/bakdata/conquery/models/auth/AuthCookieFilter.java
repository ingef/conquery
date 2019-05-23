package com.bakdata.conquery.models.auth;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

import org.eclipse.jetty.http.HttpHeader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthCookieFilter implements ContainerResponseFilter {
	
	private static final String ACCESS_TOKEN = "access_token";

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
		Cookie cookie = request.getCookies().get(ACCESS_TOKEN);
		String token = request.getUriInfo().getQueryParameters().getFirst(ACCESS_TOKEN);
		
		// Set cookie only if a token is present
		if(token != null && !token.isEmpty()) {
			if(cookie != null) {
				log.info("Overwriting {} cookie", ACCESS_TOKEN);
			}
			response.getHeaders().add(
					HttpHeader.SET_COOKIE.toString(),
					new NewCookie(ACCESS_TOKEN, token)
			);
		}
	}
}
