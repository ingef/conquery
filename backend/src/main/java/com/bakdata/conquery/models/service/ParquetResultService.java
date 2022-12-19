package com.bakdata.conquery.models.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.parquet.ResultParquetProcessor;
import com.bakdata.conquery.models.config.ParquetServiceConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultParquetResource;
import com.google.auto.service.AutoService;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.SneakyThrows;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@AutoService(Plugin.class)
public class ParquetResultService implements Plugin, ResultRendererProvider {

	private ParquetServiceConfig config = new ParquetServiceConfig();

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (config.isHidden() && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(
				ResultParquetResource.getDownloadURL(uriBuilder.clone(), (ManagedExecution<?> & SingleTableResult) exec)
		);
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
	public Optional<Class<? extends PluginConfig>> getPluginConfigClass() {
		return Optional.of(ParquetServiceConfig.class);
	}

	@Override
	public void setConfig(PluginConfig config) {
		if (config instanceof ParquetServiceConfig parquetServiceConfig) {
			this.config = parquetServiceConfig;
			return;
		}
		throw new IllegalStateException("Incompatible config provided: " + config);
	}

	@Override
	public void initialize(ManagerNode manager) {

		final JerseyEnvironment jersey = manager.getEnvironment().jersey();
		//inject required services
		jersey.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bindAsContract(ResultParquetProcessor.class);
			}
		});

		jersey.register(ResultParquetResource.class);
	}
}
