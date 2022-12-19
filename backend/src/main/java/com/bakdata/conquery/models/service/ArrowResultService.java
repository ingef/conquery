package com.bakdata.conquery.models.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.arrow.ResultArrowProcessor;
import com.bakdata.conquery.models.config.ArrowServiceConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultArrowResource;
import com.google.auto.service.AutoService;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.SneakyThrows;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@AutoService(Plugin.class)
public class ArrowResultService implements Plugin, ResultRendererProvider {

	@Valid
	@NotNull
	private ArrowServiceConfig config = new ArrowServiceConfig();

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
				ResultArrowResource.getFileDownloadURL(uriBuilder.clone(), (ManagedExecution<?> & SingleTableResult) exec),
				ResultArrowResource.getStreamDownloadURL(uriBuilder.clone(), (ManagedExecution<?> & SingleTableResult) exec)
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
		return Optional.of(ArrowServiceConfig.class);
	}

	@Override
	public void setConfig(PluginConfig config) {

		if (config instanceof ArrowServiceConfig arrowConfig) {
			this.config = arrowConfig;
			return;
		}
		throw new IllegalStateException("Incompatible config provided: " + config);
	}

	@Override
	public void initialize(ManagerNode managerNode) {

		final JerseyEnvironment jersey = managerNode.getEnvironment().jersey();

		//inject required services
		jersey.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(config).to(ArrowServiceConfig.class);
				bindAsContract(ResultArrowProcessor.class);
			}
		});

		jersey.register(ResultArrowResource.class);
	}
}
