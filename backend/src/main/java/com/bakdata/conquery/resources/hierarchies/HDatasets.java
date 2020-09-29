package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

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
		authorize(user, datasetId, Ability.READ);
		this.namespace = processor.getDatasetRegistry().get(datasetId);
		if(namespace == null) {
			throw new WebApplicationException("Could not find dataset "+datasetId, Status.NOT_FOUND);
		}
	}
}
