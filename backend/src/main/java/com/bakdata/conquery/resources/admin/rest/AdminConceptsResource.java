package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveConcept;
import com.bakdata.conquery.resources.hierarchies.HConcepts;

import lombok.Getter;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter @Setter
@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}")
public class AdminConceptsResource extends HConcepts {
	
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