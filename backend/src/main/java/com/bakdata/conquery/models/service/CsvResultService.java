package com.bakdata.conquery.models.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.csv.ResultCsvProcessor;
import com.bakdata.conquery.models.config.CsvServiceConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultCsvResource;
import com.google.auto.service.AutoService;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
@AutoService(Plugin.class)
public class CsvResultService implements Plugin, ResultRendererProvider {

	private CsvServiceConfig config = new CsvServiceConfig();

	@SneakyThrows(MalformedURLException.class)
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (config.isHidden() && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(ResultCsvResource.getDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec));
	}

	@Override
	public int getPriority() {
		return config.getPriority();
	}

	@Override
	public boolean isDefault() {
		return true;
	}

	@Override
	public Class<? extends PluginConfig> getPluginConfigClass() {
		return CsvServiceConfig.class;
	}

	@Override
	public void setConfig(PluginConfig config) {

		if (config instanceof CsvServiceConfig csvConfig) {
			this.config = csvConfig;
			return;
		}
		throw new IllegalStateException("Incompatible config provided: " + config);
	}

	@Override
	public void initialize(ManagerNode manager) {

		final JerseyEnvironment jersey = manager.getEnvironment().jersey();

		jersey.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bindAsContract(ResultCsvProcessor.class);
			}
		});
		jersey.register(ResultCsvResource.class);
	}
}
