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
import com.bakdata.conquery.io.result.parquet.ResultParquetProcessor;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultParquetResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Data;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@Data
@CPSType(base = ResultRendererProvider.class, id = "PARQUET")
public class ParquetResultProvider implements ResultRendererProvider {

	private boolean hidden = true;

	@Override
	public Collection<ResultAsset> generateResultURLs(ManagedExecution exec, UriBuilder uriBuilder, boolean allProviders)
			throws MalformedURLException, URISyntaxException {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(
				new ResultAsset("PARQUET", ResultParquetResource.getDownloadURL(uriBuilder.clone(), (ManagedExecution & SingleTableResult) exec).toURI())
		);
	}

	@Override
	public void registerResultResource(DropwizardResourceConfig jersey, ManagerNode manager) {
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
