package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.ResourceUtil;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Path("datasets/{" + DATASET + "}")
public abstract class HDatasets extends HAuthorized {

	//TODO this isn't properly injected here, inject the DSRegistry instead
	@Inject
	protected QueryProcessor processor;

	@PathParam(DATASET)
	private DatasetId datasetId;

	private Namespace namespace;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(datasetId);
		ResourceUtil.throwNotFoundIfNull(datasetId, namespace);

		authorize(user, namespace.getDataset(), Ability.READ);
	}
}
