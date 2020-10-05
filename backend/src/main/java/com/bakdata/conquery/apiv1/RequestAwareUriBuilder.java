package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.resources.ResourceConstants.API;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

public interface RequestAwareUriBuilder {

	public static UriBuilder fromRequest(HttpServletRequest request) {
		return UriBuilder.fromUri(RequestHelper.getRequestURL(request)).path(API);
	}
}
