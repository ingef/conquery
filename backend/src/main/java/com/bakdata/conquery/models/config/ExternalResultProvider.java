package com.bakdata.conquery.models.config;

import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ExternalResult;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.external.ExternalResultProcessor;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.resources.api.ResultExternalResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@Getter
@CPSType(base = ResultRendererProvider.class, id = "EXTERNAL")
public class ExternalResultProvider implements ResultRendererProvider {

	@Setter
	private boolean hidden = false;

	@Override
	public Collection<ResultAsset> generateResultURLs(ManagedExecution exec, UriBuilder uriBuilder, boolean allProviders) {

		if (!(exec instanceof ExternalResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return ((ExternalResult) exec).getResultAssets().map(assetBuilder -> assetBuilder.apply(uriBuilder.clone())).toList();
	}

	@Override
	public void registerResultResource(DropwizardResourceConfig environment, ManagerNode manager) {

		//inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bindAsContract(ExternalResultProcessor.class);
			}
		});

		environment.register(ResultExternalResource.class);
	}
}
