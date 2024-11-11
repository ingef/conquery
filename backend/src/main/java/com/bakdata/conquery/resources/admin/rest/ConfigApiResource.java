package com.bakdata.conquery.resources.admin.rest;

import java.util.Iterator;
import java.util.List;
import jakarta.inject.Inject;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.api.openapi.ConfigApi;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConfigApiResource implements ConfigApi {

	private final ManagerNode managerNode;
	private final ConqueryConfig config;

	@Override
	public String deleteFormBackend(String configId) {
		Iterator<PluginConfig> iterator = config.getPlugins().iterator();
		while (iterator.hasNext()) {
			PluginConfig next = iterator.next();
			if (next instanceof FormBackendConfig backendConfig && backendConfig.getId().equals(configId)){
				// Remove from Plugins
				iterator.remove();

				// Close client, healthcheck, ...
				backendConfig.deinitialize();
				break;
			}
		}

		// Run FormScanner
		try {
			managerNode.getFormScanner().execute(null, null);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return configId;
	}

	@Override
	public List<String> listFormBackends() {
		return List.copyOf(managerNode.getFormScanner().listFrontendFormConfigProviders());
	}

	@Override
	public String registerFormBackend(FormBackendConfig pluginConfig) {
		// We need to add this to the config, because ExternalExecution relies on this
		config.getPlugins().add(pluginConfig);

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
