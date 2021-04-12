package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;

@Setter
@Path("datasets/{" + DATASET + "}")
public abstract class HDatasets extends HAuthorized {
	
	@Inject
	protected QueryProcessor processor;
	@PathParam(DATASET)
	protected DatasetId datasetId;
	protected Namespace namespace;
	protected ObjectMapper mapper;
	
	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(datasetId);
		if(namespace == null) {
			throw new NotFoundException(String.format("Could not find Dataset[%s]",  datasetId));
		}

		authorize(user, namespace.getDataset(), Ability.READ);
	}
}
