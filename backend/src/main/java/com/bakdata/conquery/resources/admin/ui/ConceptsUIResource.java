package com.bakdata.conquery.resources.admin.ui;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.View;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

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

	@GET
	public View getConceptView() {
		return new UIView<>(
				"concept.html.ftl",
				concept
		);
	}
}