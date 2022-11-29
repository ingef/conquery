package com.bakdata.conquery.io.result.external;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.resources.api.ResultExternalResource;
import com.google.common.base.Throwables;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Provider for results that are externally generated and are proxied through the backend.
 */
@Getter
@CPSType(base = ResultRendererProvider.class, id = "EXTERNAL")
public class ExternalResultProvider implements ResultRendererProvider {

	@Setter
	private boolean hidden = false;

	/**
	 * Checks if the execution is compatible and then ask it for suitable file extensions to build a 'virtual' file name.
	 *
	 * @param exec         The execution whose result needs to be rendered.
	 * @param uriBuilder   The pre-configured builder for the url.
	 * @param allProviders A flag that should override internal "hide-this-url" flags.
	 * @return
	 */
	@Override
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {

		if (!(exec instanceof ExternalResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return ((ExternalResult) exec).getResultFileExtensions().stream()
									  .map(resultFileReference -> {
										  try {
											  return ResultExternalResource.getDownloadURL(uriBuilder.clone(), (ManagedExecution<?> & ExternalResult) exec, resultFileReference);
										  }
										  catch (MalformedURLException e) {
											  Throwables.throwIfUnchecked(e);
											  throw new IllegalStateException(e);
										  }
									  })
									  .collect(Collectors.toList());
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