package com.bakdata.conquery.models.config.auth;

import javax.ws.rs.client.ClientRequestFilter;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM)
public interface AuthenticationClientFilterProvider {

	ClientRequestFilter getFilter();
}
