package com.bakdata.conquery.models.config;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.csv.ResultCsvProcessor;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultCsvResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@CPSType(base = ResultRendererProvider.class, id = "CSV")
public class CsvResultProvider implements ResultRendererProvider {
	private boolean hidden = false;

	public Collection<ResultAsset> generateResultURLs(ManagedExecution exec, UriBuilder uriBuilder, boolean allProviders)
			throws MalformedURLException, URISyntaxException {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(new ResultAsset("CSV", ResultCsvResource.getDownloadURL(uriBuilder, (ManagedExecution & SingleTableResult) exec).toURI()));
	}

	@Override
	public void registerResultResource(DropwizardResourceConfig environment, ManagerNode manager) {

		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bindAsContract(ResultCsvProcessor.class);
			}
		});
		environment.register(ResultCsvResource.class);
	}
}
