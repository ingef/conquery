package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.ws.rs.client.ClientRequestFilter;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM)
public interface AuthenticationClientFilterProvider {

	ClientRequestFilter getFilter();
}
