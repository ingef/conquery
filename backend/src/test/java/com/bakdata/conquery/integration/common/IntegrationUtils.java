package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.ExecutionStatus;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.resources.api.QueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class IntegrationUtils {


	/**
	 * Load the constellation of roles, users and permissions into the provided storage.
	 */
	public static void importPermissionConstellation(MetaStorage storage, Role[] roles, RequiredUser[] rUsers) {

		for (Role role : roles) {
			storage.addRole(role);
		}

		for (RequiredUser rUser : rUsers) {
			User user = rUser.getUser();
			storage.addUser(user);

			RoleId[] rolesInjected = rUser.getRolesInjected();

			for (RoleId mandatorId : rolesInjected) {
				user.addRole(storage, storage.getRole(mandatorId));
			}
		}
	}


	public static void clearAuthStorage(MetaStorage storage, Role[] roles, RequiredUser[] rUsers) {
		// Clear MetaStorage
		for (Role mandator : roles) {
			storage.removeRole(mandator.getId());
		}
		for (RequiredUser rUser : rUsers) {
			storage.removeUser(rUser.getUser().getId());
		}
	}

	public static Query parseQuery(StandaloneSupport support, JsonNode rawQuery) throws JSONException, IOException {
		return ConqueryTestSpec.parseSubTree(support, rawQuery, Query.class);
	}


	public static OutputDescription copyOutput(RequiredColumn column) {
		CopyOutput out = new CopyOutput();
		out.setInputColumn(column.getName());
		out.setInputType(column.getType());
		out.setName(column.getName());
		return out;
	}

	private static URI getPostQueryURI(StandaloneSupport conquery) {
		return HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), QueryResource.class, "postQuery")
							  .buildFromMap(Map.of(
									  "dataset", conquery.getDataset().getId()
							  ));
	}

	private static JsonNode getRawExecutionStatus(String id, StandaloneSupport conquery, User user) {
		final URI queryStatusURI = getQueryStatusURI(conquery, id);
		// We try at most 5 times, queryStatus waits for 10s, we therefore don't need to timeout here.
		// Query getQueryStatus until it is no longer running.
		for (int trial = 0; trial < 5; trial++) {

			JsonNode execStatusRaw =
					conquery.getClient()
							.target(queryStatusURI)
							.request(MediaType.APPLICATION_JSON_TYPE)
							.header("Authorization", "Bearer " + conquery.getAuthorizationController().getConqueryTokenRealm().createTokenForUser(user.getId()))
							.get(JsonNode.class);

			String status = execStatusRaw.get(ExecutionStatus.Fields.status).asText();

			if (!ExecutionState.RUNNING.name().equals(status)) {
				return execStatusRaw;
			}
		}

		throw new IllegalStateException("Query was running too long.");
	}

	private static URI getQueryStatusURI(StandaloneSupport conquery, String id) {
		return HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), QueryResource.class, "getStatus")
							  .buildFromMap(Map.of(
									  "query", id, "dataset", conquery.getDataset().getId()
							  ));
	}

	/**
	 * Send a query onto the conquery instance and assert the result's size.
	 *
	 * @return
	 */
	public static ManagedExecutionId assertQueryResult(StandaloneSupport conquery, Query query, long expectedSize, ExecutionState expectedState, User user, int expectedResponseCode) {
		final URI postQueryURI = getPostQueryURI(conquery);

		final String userToken = conquery.getAuthorizationController()
											.getConqueryTokenRealm()
											.createTokenForUser(user.getId());

		Response response = conquery.getClient()
									.target(postQueryURI)
									.request(MediaType.APPLICATION_JSON_TYPE)
									.header("Authorization", "Bearer " + userToken)
									.post(Entity.entity(query, MediaType.APPLICATION_JSON_TYPE));


		assertThat(response.getStatusInfo().getStatusCode()).as("Result of %s", postQueryURI)
				.isEqualTo(expectedResponseCode);

		if (expectedState == ExecutionState.FAILED && !response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
			return null;
		}

		final JsonNode jsonNode = response.readEntity(JsonNode.class);

		final String id = jsonNode.get(ExecutionStatus.Fields.id).asText();

		// TODO implement this properly: ExecutionStatus status = response.readEntity(ExecutionStatus.Full.class);

		JsonNode execStatusRaw = getRawExecutionStatus(id, conquery, user);

		String status = execStatusRaw.get(ExecutionStatus.Fields.status).asText();
		long numberOfResults = execStatusRaw.get(ExecutionStatus.Fields.numberOfResults).asLong(0);

		assertThat(status).isEqualTo(expectedState.name());

		if (expectedState == ExecutionState.DONE && expectedSize !=  -1) {
			assertThat(numberOfResults)
					.describedAs("Query results")
					.isEqualTo(expectedSize);
		}

		return ManagedExecutionId.Parser.INSTANCE.parse(id);
	}

	public static FullExecutionStatus getExecutionStatus(StandaloneSupport conquery, ManagedExecutionId executionId, User user, int expectedResponseCode) {
		final URI queryStatusURI = getQueryStatusURI(conquery, executionId.toString());

		final String userToken = conquery.getAuthorizationController()
				.getConqueryTokenRealm()
				.createTokenForUser(user.getId());

		Response response = conquery.getClient()
				.target(queryStatusURI)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.header("Authorization", "Bearer " + userToken)
				.get();


		assertThat(response.getStatusInfo().getStatusCode()).as("Result of %s", queryStatusURI)
				.isEqualTo(expectedResponseCode);


		return response.readEntity(FullExecutionStatus.class);
	}

}
