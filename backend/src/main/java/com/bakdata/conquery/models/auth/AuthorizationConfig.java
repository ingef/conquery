package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * TODO rework doku
 * Conquery's authentication and authorization system uses this
 * interface to retrieve necessary objects and other auth related informations
 * for system for different configurations.
 * 
 * A custom authentication mechanism must implement this interface and register
 * a JSON type from this interface, before it is added to the base configuration
 * {@link ConqueryConfig}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface AuthorizationConfig {

	List<ProtoUser> getInitialUsers();

	List<String> getOverviewScope();
}
