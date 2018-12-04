package com.bakdata.conquery.io.jetty;

import java.io.IOException;
import java.util.Collections;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.google.common.net.HttpHeaders;

public class MediaTypeFixFilter implements ClientRequestFilter {

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		String contentType = requestContext.getHeaderString(HttpHeaders.CONTENT_TYPE);
		if(contentType != null && contentType.startsWith("multipart/form-data;boundary=")) {
			requestContext.getHeaders().put(HttpHeaders.CONTENT_TYPE, Collections.singletonList("multipart/form-data; boundary="+contentType.substring(29)));
		}
	}

}