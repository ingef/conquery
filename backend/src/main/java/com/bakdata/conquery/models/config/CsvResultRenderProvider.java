package com.bakdata.conquery.models.config;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRenderProvider;
import com.bakdata.conquery.io.result.csv.ResultCsvProcessor;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ResultCsvResource;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.Getter;
import lombok.SneakyThrows;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@Getter
@CPSType(base = ResultRenderProvider.class, id = "CSV")
public class CsvResultRenderProvider implements ResultRenderProvider {

	private boolean hidden = false;

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Optional<URL> generateResultURL(ManagedExecution<?> exec, UriBuilder uriBuilder) {
		if (!(exec instanceof SingleTableResult)) {
			return Optional.empty();
		}

		return Optional.of(ResultCsvResource.getDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec));
	}

	@Override
	public void registerResultResource(JerseyEnvironment environment, ManagerNode manager) {

		//inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(new ResultCsvProcessor(manager.getDatasetRegistry(), manager.getConfig())).to(ResultCsvProcessor.class);
			}
		});

		environment.register(ResultCsvResource.class);
	}
}
