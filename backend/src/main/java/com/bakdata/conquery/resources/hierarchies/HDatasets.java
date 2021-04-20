package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.ResourceUtil;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Path("datasets/{" + DATASET + "}")
public abstract class HDatasets extends HAuthorized {

	@Inject
	protected DatasetRegistry datasetRegistry;

	@PathParam(DATASET)
	private DatasetId datasetId;

	private Namespace namespace;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = datasetRegistry.get(datasetId);
		ResourceUtil.throwNotFoundIfNull(datasetId, namespace);

		user.authorize(namespace.getDataset(), Ability.READ);
	}
}
