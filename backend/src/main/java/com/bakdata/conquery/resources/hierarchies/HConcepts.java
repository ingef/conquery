package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}")
public abstract class HConcepts extends HDatasets {

	@PathParam(CONCEPT)
	protected Concept<?> concept;
	@PathParam(DATASET)
	protected Dataset dataset;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		subject.authorize(concept, Ability.READ);
	}
}