package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Path("datasets/{" + DATASET_NAME + "}")
public abstract class HDatasets extends HAuthorized {
	
	@Inject
	protected AdminProcessor processor;
	@PathParam(DATASET_NAME)
	protected DatasetId datasetId;
	protected Namespace namespace;
	protected ObjectMapper mapper;
	
	@PostConstruct
	@Override
	public void init() {
		super.init();
		authorize(user, datasetId, Ability.READ);
		this.namespace = processor.getNamespaces().get(datasetId);
		if(namespace == null) {
			throw new WebApplicationException("Could not find dataset "+datasetId, Status.NOT_FOUND);
		}
	}
}
