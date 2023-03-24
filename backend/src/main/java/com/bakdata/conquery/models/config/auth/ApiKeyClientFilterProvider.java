package com.bakdata.conquery.models.config.auth;


import javax.validation.constraints.NotEmpty;
import javax.ws.rs.client.ClientRequestFilter;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonIgnore;


@CPSType(id = "API_KEY", base = AuthenticationClientFilterProvider.class)
public record ApiKeyClientFilterProvider(@NotEmpty String apiKey) implements AuthenticationClientFilterProvider {

	private static final String HEADER = "X-API-KEY";

	@JsonIgnore
	@Override
	public ClientRequestFilter getFilter() {
		return requestContext -> requestContext.getHeaders().add(HEADER, apiKey);
	}
}
