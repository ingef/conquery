package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.QUERY;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.ResourceUtil;
import com.fasterxml.jackson.databind.JsonNode;

import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;

@Path("datasets/{" + DATASET + "}/stored-queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@PermitAll
public class StoredQueriesResource {

	private final StoredQueriesProcessor processor;
	private final ResourceUtil dsUtil;

	public StoredQueriesResource(Namespaces namespaces) {
		this.processor = new StoredQueriesProcessor(namespaces);
		this.dsUtil = new ResourceUtil(namespaces);
	}

	@GET
	public List<SQStatus> getAllQueries(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @Context HttpServletRequest req) {
		authorize(user, datasetId, Ability.READ);

		return processor.getAllQueries(dsUtil.getDataset(datasetId), req).stream()
				.filter(status -> user.isPermitted(new QueryPermission(user.getId(), Ability.READ.asSet(), status.getId())))
				.collect(Collectors.toList());
	}

	@GET
	@Path("{" + QUERY + "}")
	public SQStatus getQueryWithSource(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId) {
		Dataset dataset = dsUtil.getDataset(datasetId);
		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.READ);

		return processor.getQueryWithSource(dsUtil.getDataset(datasetId), queryId);
	}

	@PATCH
	@Path("{" + QUERY + "}")
	public SQStatus patchQuery(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId, JsonNode patch) throws JSONException {
		authorize(user, datasetId, Ability.READ);

		Dataset dataset = dsUtil.getDataset(datasetId);

		processor.patchQuery(user, dataset, queryId, patch);
		return getQueryWithSource(user, datasetId, queryId);
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public void deleteQuery(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(QUERY) ManagedQueryId queryId) {

		authorize(user, datasetId, Ability.READ);
		authorize(user, queryId, Ability.DELETE);

		processor.deleteQuery(dsUtil.getDataset(datasetId), dsUtil.getManagedQuery(datasetId, queryId));
	}
}
