package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.StoredQueriesProcessor;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.hierarchies.HDatasets;
import com.bakdata.conquery.util.ResourceUtil;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;

@Path("datasets/{" + DATASET + "}/stored-queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)

public class StoredQueriesResource extends HDatasets{

	private final StoredQueriesProcessor processor;
	private final ResourceUtil dsUtil;

	public StoredQueriesResource(Namespaces namespaces) {
		this.processor = new StoredQueriesProcessor(namespaces);
		this.dsUtil = new ResourceUtil(namespaces);
	}

	@GET
	public List<ExecutionStatus> getAllQueries(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @Context HttpServletRequest req) {
		return processor.getAllQueries(dsUtil.getDataset(datasetId), req, user)
			.collect(Collectors.toList());
	}

	@GET
	@Path("{" + QUERY + "}")
	public ExecutionStatus getQueryWithSource(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId) {
		Dataset dataset = dsUtil.getDataset(datasetId);
		authorize(user, queryId, Ability.READ);

		ExecutionStatus status = processor.getQueryWithSource(dataset, queryId, user);
		if (status == null) {
			throw new WebApplicationException("Unknown query " + queryId, Status.NOT_FOUND);
		}
		return status;
	}

	@PATCH
	@Path("{" + QUERY + "}")
	public ExecutionStatus patchQuery(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId, MetaDataPatch patch) {
		
		processor.patchQuery(user, queryId, patch);
		
		return getQueryWithSource(user, datasetId, queryId);
	}


	@DELETE
	@Path("{" + QUERY + "}")
	public void deleteQuery(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId) {

		authorize(user, queryId, Ability.DELETE);

		processor.deleteQuery(dsUtil.getDataset(datasetId), dsUtil.getManagedQuery(queryId));
	}
}
