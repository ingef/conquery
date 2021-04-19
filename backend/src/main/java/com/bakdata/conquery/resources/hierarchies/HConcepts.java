package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.ResourceUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}")
public abstract class HConcepts extends HDatasets {

	@PathParam(CONCEPT)
	protected ConceptId conceptId;
	protected Concept<?> concept;
	@PathParam(DATASET)
	protected DatasetId datasetId;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.concept = getNamespace().getStorage().getConcept(conceptId);
		ResourceUtil.throwNotFoundIfNull(conceptId, concept);

		user.authorize(concept, Ability.READ);
	}
}