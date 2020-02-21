package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.QUERY;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorizeReadDatasets;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.util.ResourceUtil;
import io.dropwizard.auth.Auth;

@Path("datasets/{" + DATASET + "}/queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)

public class QueryResource {
	
	private QueryProcessor processor;
	private ResourceUtil dsUtil;
	
	@Inject
	public QueryResource(QueryProcessor processor) {
		this.processor= processor;
		dsUtil = new ResourceUtil(processor.getNamespaces());
	}

	@POST
	public ExecutionStatus postQuery(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @NotNull @Valid SubmittedQuery query, @Context HttpServletRequest req) throws JSONException {
		authorize(user, datasetId, Ability.READ);
		// Also look into the query and check the datasets
		authorizeReadDatasets(user, query);
		// Check reused query
//		for (ManagedExecutionId requiredQueryId : query
//			.collectRequiredQueries()) {
//			authorize(user, requiredQueryId, Ability.READ);
//		}

		return processor.postQuery(
			dsUtil.getDataset(datasetId),
			query,
			URLBuilder.fromRequest(req),
			user);
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public ExecutionStatus cancel(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId, @Context HttpServletRequest req) throws SQLException {
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);

		return processor.cancel(
			dsUtil.getDataset(datasetId),
			dsUtil.getManagedQuery(queryId),
			URLBuilder.fromRequest(req));
	}

	@GET
	@Path("{" + QUERY + "}")
	public ExecutionStatus getStatus(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedExecutionId queryId, @Context HttpServletRequest req) throws InterruptedException {
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);
		ManagedExecution query = dsUtil.getManagedQuery(queryId);
		query.awaitDone(10, TimeUnit.SECONDS);
		return processor.getStatus(
			dsUtil.getDataset(datasetId),
			query,
			URLBuilder.fromRequest(req),
			user);
	}
}
