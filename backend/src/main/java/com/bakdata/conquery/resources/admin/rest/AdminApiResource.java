package com.bakdata.conquery.resources.admin.rest;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.api.openapi.BusyApi;
import com.bakdata.conquery.models.api.openapi.ConfigApi;
import com.bakdata.conquery.models.api.openapi.ScriptApi;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminApiResource implements ConfigApi, BusyApi, ScriptApi {


	private final AdminProcessor processor;
	private final ManagerNode managerNode;
	private final ConqueryConfig config;


	@HeaderParam(HttpHeaders.ACCEPT)
	private String acceptHeader;

	@Override
	public String deleteFormBackend(String configId) {
		Iterator<PluginConfig> iterator = config.getPlugins().iterator();
		while (iterator.hasNext()) {
			PluginConfig next = iterator.next();
			if (next instanceof FormBackendConfig backendConfig && backendConfig.getId().equals(configId)){
				// Remove from Plugins
				iterator.remove();

				// Close client, healthcheck, ...
				backendConfig.close();
				break;
			}
		}

		// Run FormScanner
		managerNode.getFormScanner().execute(null, null);

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

		managerNode.getFormScanner().execute(null, null);

		return pluginConfig.getId();
	}

	@Override
	public Boolean busyGet() {
		return processor.isBusy();
	}

	@Override
	public Object executeScript(String script) {
		return switch(acceptHeader) {
			case MediaType.TEXT_PLAIN -> Objects.toString(processor.executeScript(script));
			case MediaType.APPLICATION_JSON -> processor.executeScript(script);
			default -> throw new IllegalArgumentException("Unexpected value: " + acceptHeader);
		};
	}
}
