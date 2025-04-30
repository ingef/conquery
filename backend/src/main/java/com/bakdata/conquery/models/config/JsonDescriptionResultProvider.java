package com.bakdata.conquery.models.config;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.json.ResultJsonDescriptionProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.resources.api.ResultJsonDescriptionResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
@Data
@CPSType(base = ResultRendererProvider.class, id = "JSON")
public class JsonDescriptionResultProvider implements ResultRendererProvider {
	private boolean hidden = false;

	public Collection<ResultAsset> generateResultURLs(ManagedExecution exec, Subject viewer, UriBuilder uriBuilder, boolean allProviders)
			throws MalformedURLException, URISyntaxException {

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(new ResultAsset("JSON", ResultJsonDescriptionResource.getDownloadURL(uriBuilder, exec).toURI()));
	}

	@Override
	public void registerResultResource(DropwizardResourceConfig environment, ManagerNode manager) {

		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bindAsContract(ResultJsonDescriptionProcessor.class);
			}
		});
		environment.register(ResultJsonDescriptionResource.class);
	}

}
