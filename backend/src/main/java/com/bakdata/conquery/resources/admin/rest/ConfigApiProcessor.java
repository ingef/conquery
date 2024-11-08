package com.bakdata.conquery.resources.admin.rest;

import jakarta.inject.Inject;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.bakdata.conquery.models.config.ModelApiResponse;
import org.openapitools.api.ConfigApi;

public class ConfigApiProcessor implements ConfigApi {

	@Inject
	private ManagerNode managerNode;

	@Override
	public ModelApiResponse addPlugin(FormBackendConfig pluginConfig) {
		pluginConfig.initialize(managerNode);
		return null;
	}
}
