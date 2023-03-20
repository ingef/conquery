package com.bakdata.conquery.models.config;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.arrow.ResultArrowProcessor;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultArrowResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Data;
import lombok.SneakyThrows;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Data
@CPSType(base = ResultRendererProvider.class, id = "ARROW")
public class ArrowResultProvider implements ResultRendererProvider {
	private boolean hidden = true;

	@Valid
	@NotNull
	private ArrowConfig config = new ArrowConfig();

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<ResultAsset> generateResultURLs(ManagedExecution exec, UriBuilder uriBuilder, boolean allProviders) {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(
				new ResultAsset("Arrow File", ResultArrowResource.getFileDownloadURL(uriBuilder.clone(), (ManagedExecution & SingleTableResult) exec)),
				new ResultAsset("Arrow Stream", ResultArrowResource.getStreamDownloadURL(uriBuilder.clone(), (ManagedExecution & SingleTableResult) exec))
		);
	}


	@Override
	public void registerResultResource(DropwizardResourceConfig jersey, ManagerNode manager) {

		//inject required services
		jersey.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(config).to(ArrowConfig.class);
				bindAsContract(ResultArrowProcessor.class);
			}
		});

		jersey.register(ResultArrowResource.class);
	}
}
