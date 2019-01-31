package com.bakdata.conquery.models.auth;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;
import com.bakdata.conquery.io.jersey.AuthCookie;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import org.eclipse.jetty.http.HttpHeader;

@Provider
@AuthCookie
public class AuthCookieFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        MultivaluedMap<String, String> pathParameters = request.getUriInfo().getQueryParameters();
        Cookie accessTokenCookie = new NewCookie("access_token", pathParameters.getFirst("access_token"));
        
        response.getHeaders().add(HttpHeader.COOKIE.toString(), accessTokenCookie);
    }

    
}
