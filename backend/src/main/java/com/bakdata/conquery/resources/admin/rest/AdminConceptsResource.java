package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
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

	private final AdminDatasetProcessor processor;

	@PathParam(DATASET)
	private DatasetId dataset;
	@PathParam(CONCEPT)
	private ConceptId concept;

	private Namespace namespace;


	@PostConstruct
	public void init() {
		namespace = processor.getDatasetRegistry().get(dataset);
	}

	@GET
	public Concept<?> getConcept() {
		return concept.resolve();
	}

	@DELETE
	public void removeConcept() {
		processor.deleteConcept(concept);
	}
}