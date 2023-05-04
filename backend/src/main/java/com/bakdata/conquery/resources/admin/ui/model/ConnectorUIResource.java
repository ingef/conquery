package com.bakdata.conquery.resources.admin.ui.model;

import static com.bakdata.conquery.resources.ResourceConstants.CONNECTOR;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import io.dropwizard.views.View;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/connectors/{" + CONNECTOR + "}")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConnectorUIResource {

	protected final UIProcessor uiProcessor;

	@PathParam(CONNECTOR)
	protected Connector connector;
	@PathParam(DATASET)
	protected Dataset dataset;

	@GET
	public View getConnectorView() {
		return new UIView<>(
				"connector.html.ftl",
				connector
		);
	}
}