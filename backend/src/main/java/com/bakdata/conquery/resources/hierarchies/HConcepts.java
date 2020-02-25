package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}")
public abstract class HConcepts extends HDatasets {
	
	@PathParam(CONCEPT_NAME)
	protected ConceptId conceptId;
	protected Concept<?> concept;
	@PathParam(DATASET_NAME)
	protected DatasetId datasetId;
	
	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.concept = namespace.getStorage().getConcept(conceptId);
		if(this.concept == null) {
			throw new WebApplicationException("Could not find concept "+conceptId, Status.NOT_FOUND);
		}
	}
}