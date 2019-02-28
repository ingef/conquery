package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.QUERY;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

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

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.ResourceUtil;

import io.dropwizard.auth.Auth;

@Path("datasets/{" + DATASET + "}/queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@PermitAll
public class QueryResource {

	private final QueryProcessor processor;
	private final ResourceUtil dsUtil;

	public QueryResource(Namespaces namespaces, MasterMetaStorage storage) {
		this.processor = new QueryProcessor(namespaces, storage);
		this.dsUtil = new ResourceUtil(namespaces);
	}

	@POST
	public SQStatus postQuery(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @NotNull @Valid IQuery query, @Context HttpServletRequest req) throws JSONException {
		authorize(user, datasetId, Ability.READ);
		// Check reused query
		for (ManagedQueryId requiredQueryId : query.collectRequiredQueries()) {
			authorize(user, requiredQueryId, Ability.READ);
		}

		return processor.postQuery(dsUtil.getDataset(datasetId), query, URLBuilder.fromRequest(req), user);
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public SQStatus cancel(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId, @Context HttpServletRequest req) throws SQLException {
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);

		return processor.cancel(dsUtil.getDataset(datasetId), dsUtil.getManagedQuery(datasetId, queryId), URLBuilder.fromRequest(req));
	}

	@GET
	@Path("{" + QUERY + "}")
	public SQStatus getStatus(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId, @Context HttpServletRequest req) throws InterruptedException {
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);

		return processor.getStatus(dsUtil.getDataset(datasetId), dsUtil.getManagedQuery(datasetId, queryId), URLBuilder.fromRequest(req));
	}
}
