package com.bakdata.conquery.io.result.ResultRender;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jersey.setup.JerseyEnvironment;

import javax.ws.rs.core.UriBuilder;
import java.net.URL;
import java.util.Collection;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface ResultRendererProvider {

	/**
	 * The provider can return a result url if its renderer supports the execution type.
	 * If additionally allProviders is set to true it should output an url.
	 * @param exec The execution whose result needs to be rendered.
	 * @param uriBuilder The pre-configured builder for the url.
	 * @param allProviders A flag that should override internal "hide-this-url" flags.
	 * @return An Optional with the url or an empty optional.
	 */
	Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders);

	void registerResultResource(JerseyEnvironment environment, ManagerNode manager);
}
