package com.bakdata.conquery.resources.admin.rest;

import java.util.List;
import jakarta.inject.Inject;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.api.openapi.ConfigApi;
import com.bakdata.conquery.models.config.FormBackendConfig;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConfigApiResource implements ConfigApi {

	private final ManagerNode managerNode;

	@Override
	public String deleteFormBackend(String configId) {
		String providerName = managerNode.getFormScanner().unregisterFrontendFormConfigProvider(configId).getProviderName();
		try {
			managerNode.getFormScanner().execute(null, null);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return providerName;
	}

	@Override
	public List<String> listFormBackends() {
		return List.copyOf(managerNode.getFormScanner().listFrontendFormConfigProviders());
	}

	@Override
	public String registerFormBackend(FormBackendConfig pluginConfig) {
		pluginConfig.initialize(managerNode);
		try {
			managerNode.getFormScanner().execute(null, null);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return pluginConfig.getId();
	}
}
