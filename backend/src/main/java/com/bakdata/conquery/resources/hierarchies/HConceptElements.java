package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}")
public abstract class HConceptElements extends HDatasets {
	
	@PathParam(CONCEPT_NAME)
	protected ConceptElementId<?> conceptElementId;
	protected ConceptElement<?> conceptElement;
	protected Concept<?> concept;
	
	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.concept = namespace.getStorage().getConcept(conceptElementId.findConcept());
		if(this.concept == null) {
			throw new WebApplicationException("Could not find concept "+conceptElementId.findConcept(), Status.NOT_FOUND);
		}
		this.conceptElement = concept.getElementById(conceptElementId);
		if(this.conceptElement == null) {
			throw new WebApplicationException("Could not find concept element "+conceptElementId, Status.NOT_FOUND);
		}
	}
}