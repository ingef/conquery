package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.apiv1.StoredQueriesProcessor;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.FullExecutionStatus;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.resources.hierarchies.HDatasets;
import io.dropwizard.jersey.PATCH;

@Path("datasets/{" + DATASET + "}/stored-queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)

public class StoredQueriesResource extends HDatasets {

	@Inject
	private StoredQueriesProcessor processor;

	@GET
	public List<ExecutionStatus> getAllQueries(DatasetId datasetId) {
		return processor.getAllQueries(getNamespace(), servletRequest, user)
						.collect(Collectors.toList());
	}

	@GET
	@Path("{" + QUERY + "}")
	public FullExecutionStatus getSingleQueryInfo(@PathParam(QUERY) ManagedExecutionId queryId) {
		return processor.getQueryFullStatus(queryId, user, RequestAwareUriBuilder.fromRequest(servletRequest));
	}

	@PATCH
	@Path("{" + QUERY + "}")
	public FullExecutionStatus patchQuery(@PathParam(QUERY) ManagedExecutionId queryId, MetaDataPatch patch) throws JSONException {
		processor.patchQuery(user, queryId, patch);
		
		return processor.getQueryFullStatus(queryId, user, RequestAwareUriBuilder.fromRequest(servletRequest));
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public void deleteQuery(@PathParam(QUERY) ManagedExecutionId queryId) {
		processor.deleteQuery(user, queryId);
	}
}
