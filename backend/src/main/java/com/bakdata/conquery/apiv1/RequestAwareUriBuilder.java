package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.resources.ResourceConstants.API;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestAwareUriBuilder {

	public UriBuilder fromRequest(HttpServletRequest request) {
		return UriBuilder.fromUri(RequestHelper.getRequestURL(request)).path(API);
	}
}
