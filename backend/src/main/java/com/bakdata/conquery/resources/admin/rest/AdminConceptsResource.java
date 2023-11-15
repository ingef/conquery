package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AdminConceptsResource {

	private final AdmDatasetProcessor processor;

	@PathParam(DATASET)
	private Dataset dataset;
	private Namespace namespace;
	@PathParam(CONCEPT)
	private Concept concept;

	@PostConstruct
	public void init() {
		namespace = processor.getDatasetRegistry().get(dataset.getId());
	}

	@GET
	public Concept getConcept() {
		return concept;
	}

	@DELETE
	public void removeConcept() {
		processor.deleteConcept(concept);
	}
}