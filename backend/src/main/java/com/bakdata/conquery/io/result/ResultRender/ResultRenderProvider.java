package com.bakdata.conquery.io.result.ResultRender;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jersey.setup.JerseyEnvironment;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface ResultRenderProvider {

	boolean isHidden();

	Optional<URL> generateResultURL(ManagedExecution<?> exec, UriBuilder uriBuilder);

	void registerResultResource(JerseyEnvironment environment, ManagerNode manager);
}
