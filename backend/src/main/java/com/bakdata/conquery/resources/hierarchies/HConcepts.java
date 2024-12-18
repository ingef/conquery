package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}")
public abstract class HConcepts extends HDatasets {

	@PathParam(CONCEPT)
	protected ConceptId concept;
	@PathParam(DATASET)
	protected DatasetId dataset;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		subject.authorize(concept, Ability.READ);
	}
}