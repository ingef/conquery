package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Path("datasets/{" + DATASET + "}")
public abstract class HDatasets extends HAuthorized {

	@Inject
	protected DatasetRegistry<?> datasetRegistry;

	@PathParam(DATASET)
	private Dataset dataset;

	private Namespace namespace;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = datasetRegistry.get(dataset.getId());
		subject.authorize(dataset, Ability.READ);
	}
}
