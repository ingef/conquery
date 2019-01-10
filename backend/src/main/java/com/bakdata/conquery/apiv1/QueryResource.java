package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.QUERY;

import java.sql.SQLException;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.QueryExecutionException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.ResourceUtil;

import io.dropwizard.auth.Auth;

@Path("datasets/{" + DATASET + "}/queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@PermitAll
public class QueryResource {

	private QueryProcessor processor;
	private ResourceUtil resourceUtil;

	public QueryResource(Namespaces namespaces, ConqueryConfig config) {
		this.processor = new QueryProcessor(namespaces, config);
		this.resourceUtil = new ResourceUtil(namespaces);
	}

	@POST
	public SQStatus postQuery(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @NotNull @Valid IQuery query, @Context HttpServletRequest req) throws QueryExecutionException, JSONException {
		return processor.postQuery(resourceUtil.getDataset(datasetId), query, URLBuilder.fromRequest(req), user);
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public SQStatus cancel(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId, @Context HttpServletRequest req) throws SQLException {

		Dataset dataset = resourceUtil.getDataset(datasetId);
		ManagedQuery query = resourceUtil.getManagedQuery(datasetId, queryId);

		return processor.cancel(user, dataset, query, URLBuilder.fromRequest(req));
	}

	@GET
	@Path("{" + QUERY + "}")
	public SQStatus getStatus(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId, @Context HttpServletRequest req) throws InterruptedException {

		Dataset dataset = resourceUtil.getDataset(datasetId);
		ManagedQuery query = resourceUtil.getManagedQuery(datasetId, queryId);

		return processor.getStatus(user, dataset, query, URLBuilder.fromRequest(req));
	}
}
