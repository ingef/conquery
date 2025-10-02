package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.execution.OverviewExecutionStatus;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.resources.api.DatasetQueryResource;
import com.bakdata.conquery.resources.api.QueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class IntegrationUtils {


	public static Stream<Bucket> getAllBuckets(WorkerStorage workerStorage) {
		return workerStorage.getAllBucketIds().map(workerStorage::getBucket);
	}


	public static Query parseQuery(StandaloneSupport support, JsonNode rawQuery) throws JSONException, IOException {
		return ConqueryTestSpec.parseSubTree(support, rawQuery, Query.class, false);
	}

	/**
	 * Send a query onto the conquery instance and assert the result's size.
	 */
	public static ManagedExecutionId assertQueryResult(StandaloneSupport conquery, Object query, long expectedSize, ExecutionState expectedState, @Nullable User user, int expectedResponseCode) {
		final URI postQueryURI = getPostQueryURI(conquery);


		// Submit Query
		Invocation.Builder request = conquery.getClient()
											 .target(postQueryURI)
											 .request(MediaType.APPLICATION_JSON_TYPE);

		if (user != null) {
			// Override authentication if user is provided
			final String userToken = conquery.getAuthorizationController()
											 .getConqueryTokenRealm()
											 .createTokenForUser(user.getId());

			request.header("Authorization", "Bearer " + userToken);
		}

		try(final Response response = request
										  .post(Entity.entity(query, MediaType.APPLICATION_JSON_TYPE))) {

			assertThat(response.getStatusInfo().getStatusCode())
					.as(() -> response.readEntity(String.class))
					.isEqualTo(expectedResponseCode);

			if (expectedState == ExecutionState.FAILED && !response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
				return null;
			}

			// TODO implement this properly: ExecutionStatus status = response.readEntity(ExecutionStatus.Full.class);
			final JsonNode jsonNode = response.readEntity(JsonNode.class);
			final String id = jsonNode.get(ExecutionStatus.Fields.id).asText();

			final JsonNode execStatusRaw = getRawExecutionStatus(id, conquery, user);

			final String status = execStatusRaw.get(ExecutionStatus.Fields.status).asText();
			final long numberOfResults = execStatusRaw.get(ExecutionStatus.Fields.numberOfResults).asLong(0);

			assertThat(status).isEqualTo(expectedState.name());

			if (expectedState == ExecutionState.DONE && expectedSize != -1) {
				assertThat(numberOfResults)
						.describedAs("Query results")
						.isEqualTo(expectedSize);
			}

			return ManagedExecutionId.Parser.INSTANCE.parse(id);
		}
	}

	public static List<OverviewExecutionStatus> getAllQueries(StandaloneSupport conquery, int expectedResponseCode) {

		URI allQueriesURI = getAllQueriesURI(conquery);
		final Response response = conquery.getClient()
										  .target(allQueriesURI)
										  .queryParam("all-providers", true)
										  .request(MediaType.APPLICATION_JSON_TYPE)
										  .get();


		assertThat(response.getStatusInfo().getStatusCode()).as(() -> response.readEntity(String.class))
															.isEqualTo(expectedResponseCode);

		return response.readEntity(new GenericType<>() {
		});
	}

	public static URI getPostQueryURI(StandaloneSupport conquery) {
		return HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), DatasetQueryResource.class, "postQuery")
							  .buildFromMap(Map.of(
									  "dataset", conquery.getDataset()
							  ));
	}

	private static URI getAllQueriesURI(StandaloneSupport conquery) {
		return HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), DatasetQueryResource.class, "getAllQueries")
							  .buildFromMap(Map.of(
									  "dataset", conquery.getDataset()
							  ));
	}

	private static JsonNode getRawExecutionStatus(String id, StandaloneSupport conquery, User user) {
		final URI queryStatusURI = getQueryStatusURI(conquery, id);
		// We try at most 5 times, queryStatus waits for 10s, we therefore don't need to timeout here.
		// Query getQueryStatus until it is no longer running.
		for (int trial = 0; trial < 5; trial++) {
			log.debug("Trying to get Query result");

			final JsonNode execStatusRaw =
					conquery.getClient()
							.target(queryStatusURI)
							.request(MediaType.APPLICATION_JSON_TYPE)
							.header("Authorization", "Bearer " + conquery.getAuthorizationController().getConqueryTokenRealm().createTokenForUser(user.getId()))
							.get(JsonNode.class);

			final String status = execStatusRaw.get(ExecutionStatus.Fields.status).asText();

			if (!ExecutionState.RUNNING.name().equals(status)) {
				return execStatusRaw;
			}
		}

		throw new IllegalStateException("Query was running too long.");
	}

	private static URI getQueryStatusURI(StandaloneSupport conquery, String id) {
		return HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), QueryResource.class, "getStatus")
							  .buildFromMap(Map.of(
									  "query", id, "dataset", conquery.getDataset()
							  ));
	}

	private static URI getQueryCancelURI(StandaloneSupport conquery, String id) {
		return HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), QueryResource.class, "cancel")
							  .buildFromMap(Map.of(
									  "query", id, "dataset", conquery.getDataset()
							  ));
	}

	public static FullExecutionStatus getExecutionStatus(StandaloneSupport conquery, ManagedExecutionId executionId, User user, int expectedResponseCode) {
		final URI queryStatusURI = getQueryStatusURI(conquery, executionId.toString());

		final String userToken = conquery.getAuthorizationController()
										 .getConqueryTokenRealm()
										 .createTokenForUser(user.getId());

		final Response response = conquery.getClient()
										  .target(queryStatusURI)
										  .request(MediaType.APPLICATION_JSON_TYPE)
										  .header("Authorization", "Bearer " + userToken)
										  .get();


		assertThat(response.getStatusInfo().getStatusCode()).as("Result of %s", queryStatusURI)
															.isEqualTo(expectedResponseCode);


		return response.readEntity(FullExecutionStatus.class);
	}

	public static Response cancelQuery(StandaloneSupport conquery, ManagedExecutionId executionId, User user) {
		final URI cancelQueryURI = getQueryCancelURI(conquery, executionId.toString());

		final String userToken = conquery.getAuthorizationController()
										 .getConqueryTokenRealm()
										 .createTokenForUser(user.getId());

		return conquery.getClient()
										  .target(cancelQueryURI)
										  .request(MediaType.APPLICATION_JSON_TYPE)
										  .header("Authorization", "Bearer " + userToken)
										  .post(null);

	}
}
