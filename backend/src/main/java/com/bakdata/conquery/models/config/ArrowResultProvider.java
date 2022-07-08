package com.bakdata.conquery.models.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

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
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@Data
@CPSType(base = ResultRendererProvider.class, id = "ARROW")
public class ArrowResultProvider implements ResultRendererProvider {
	private boolean hidden = true;

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(
				ResultArrowResource.getStreamDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec),
				ResultArrowResource.getFileDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec)
		);
	}


	@Override
	public void registerResultResource(DropwizardResourceConfig jersey, ManagerNode manager) {

		//inject required services
		jersey.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bindAsContract(ResultArrowProcessor.class);
			}
		});

		jersey.register(ResultArrowResource.class);
	}
}
