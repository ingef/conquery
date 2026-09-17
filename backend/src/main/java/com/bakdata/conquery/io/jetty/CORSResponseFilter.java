package com.bakdata.conquery.io.jetty;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

public class CORSResponseFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext rCtx) {
		rCtx.getHeaders().add("Access-Control-Allow-Origin", "*");
		rCtx.getHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, If-None-Match");
		rCtx.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PATCH,DELETE, PUT");
		rCtx.getHeaders().add("Access-Control-Expose-Headers", "ETag");
	}

}
