package com.bakdata.conquery.resources.admin.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.config.FormBackendConfig;
import org.openapitools.api.ConfigApiService;
import org.openapitools.api.NotFoundException;

public class AdminConfigProcessor extends ConfigApiService {

	@Inject
	private ManagerNode managerNode;

	@Override
	public Response addPlugin(FormBackendConfig pluginConfig, SecurityContext securityContext) throws NotFoundException {
		pluginConfig.initialize(managerNode);
		return Response.noContent().build();
	}
}
