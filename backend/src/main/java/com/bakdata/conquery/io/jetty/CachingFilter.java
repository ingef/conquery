package com.bakdata.conquery.io.jetty;

import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

public class CachingFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext rCtx) {
		
		//if request where caching would make sense
		if(requestContext.getMethod().equals(HttpMethod.GET)) {
			rCtx.getHeaders().add(HttpHeaders.CACHE_CONTROL, "private");
			rCtx.getHeaders().add(HttpHeaders.CACHE_CONTROL, "must-revalidate");
			
			//if no etag that means we actually want the browser to cache
			if(rCtx.getEntityTag()==null) {
				rCtx.getHeaders().add(HttpHeaders.CACHE_CONTROL, "no-cache");
				rCtx.getHeaders().add(HttpHeaders.CACHE_CONTROL, "no-store");
				rCtx.getHeaders().add(HttpHeaders.PRAGMA, "no-cache");
				rCtx.getHeaders().add(HttpHeaders.EXPIRES, 0);
			}
		}
	}

}