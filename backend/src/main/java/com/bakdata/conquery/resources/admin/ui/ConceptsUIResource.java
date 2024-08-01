package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenSetFilter;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConceptsUIResource {

	protected final UIProcessor uiProcessor;

	@PathParam(CONCEPT)
	protected Concept<?> concept;
	@PathParam(DATASET)
	protected Dataset dataset;
	@Context
	ContainerRequestContext request;

	@GET
	public View getConceptView() {
		return new UIView<>(
				"concept.html.ftl",
				uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(request)),
				concept
		);
	}
}