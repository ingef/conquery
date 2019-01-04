package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.*;

import java.sql.SQLException;

import javax.annotation.security.PermitAll;
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
		return processor.postQuery(resourceUtil.getDataset(datasetId), query, URLBuilder.fromRequest(req));
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public SQStatus cancel(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId, @Context HttpServletRequest req) throws SQLException {

		Dataset dataset = resourceUtil.getDataset(datasetId);
		ManagedQuery query = resourceUtil.getManagedQuery(datasetId, queryId);

		return processor.cancel(dataset, query, URLBuilder.fromRequest(req));
	}

	@GET
	@Path("{" + QUERY + "}")
	public SQStatus getStatus(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId, @Context HttpServletRequest req) throws InterruptedException {

		Dataset dataset = resourceUtil.getDataset(datasetId);
		ManagedQuery query = resourceUtil.getManagedQuery(datasetId, queryId);

		return processor.getStatus(dataset, query, URLBuilder.fromRequest(req));
	}
}
