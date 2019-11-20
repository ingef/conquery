package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveConcept;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.hierarchies.HAdmin;

import lombok.Getter;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter @Setter
@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}")
public class AdminConceptsResource extends HAdmin {
	
	@PathParam(DATASET_NAME)
	protected DatasetId datasetId;
	protected Namespace namespace;
	@PathParam(CONCEPT_NAME)
	protected ConceptId conceptId;
	protected Concept<?> concept;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getNamespaces().get(datasetId);
		this.concept = namespace.getStorage().getConcept(conceptId);
		if (this.concept == null) {
			throw new WebApplicationException("Could not find concept " + conceptId, Status.NOT_FOUND);
		}
	}

	@DELETE
	public void removeConcept() {
		processor
			.getJobManager()
			.addSlowJob(new SimpleJob("Adding concept " + conceptId, () -> namespace.getStorage().removeConcept(conceptId)));
		processor
			.getJobManager()
			.addSlowJob(new SimpleJob("sendToAll: remove " + conceptId, () -> namespace.sendToAll(new RemoveConcept(conceptId))));
	}
}