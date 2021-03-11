package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.resources.api.QueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IntegrationUtils {


	/**
	 * Load the constellation of roles, users and permissions into the provided storage.
	 */
	public static void importPermissionConstellation(MetaStorage storage, Role[] roles,  RequiredUser[] rUsers) {

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

	public static IQuery parseQuery(StandaloneSupport support, JsonNode rawQuery) throws JSONException, IOException {
		return ConqueryTestSpec.parseSubTree(support, rawQuery, IQuery.class);
	}
	


	public static OutputDescription copyOutput(RequiredColumn column) {
		CopyOutput out = new CopyOutput();
		out.setInputColumn(column.getName());
		out.setInputType(column.getType());
		out.setName(column.getName());
		return out;
	}

	/**
	 * Send a query onto the conquery instance and assert the result's size.
	 */
	public static void assertQueryResult(StandaloneSupport conquery, IQuery query, long size, ExecutionState expectedState) {
		final URI postQueryURI = getPostQueryURI(conquery);

		Response response = conquery.getClient()
									.target(postQueryURI)
									.request(MediaType.APPLICATION_JSON_TYPE)
									.post(Entity.entity(query, MediaType.APPLICATION_JSON_TYPE));

		if (response.getStatusInfo().getFamily().equals(Response.Status.Family.familyOf(400)) && expectedState.equals(ExecutionState.FAILED)) {
			return;
		}

		final JsonNode jsonNode = response.readEntity(JsonNode.class);

		final String id = jsonNode.get(ExecutionStatus.Fields.id).asText();
		String status = jsonNode.get(ExecutionStatus.Fields.status).asText();
		long numberOfResults = 0;

		// TODO implement this properly: ExecutionStatus status = response.readEntity(ExecutionStatus.Full.class);

		final URI queryStatusURI = getQueryStatusURI(conquery, id);

		// We try at most 5 times, queryStatus waits for 10s, we therefore don't need to timeout here.
		// Query getQueryStatus until it is no longer running.
		for (int trial = 0; trial < 5; trial++) {

			final JsonNode currentStatus =
					conquery.getClient()
							.target(queryStatusURI)
							.request(MediaType.APPLICATION_JSON_TYPE)
							.get(JsonNode.class);

			status = currentStatus.get(ExecutionStatus.Fields.status).asText();
			numberOfResults = currentStatus.get(ExecutionStatus.Fields.numberOfResults).asLong(0);

			if (!ExecutionState.RUNNING.name().equals(status)) {
				break;
			}
		}

		assertThat(status).isEqualTo(expectedState.name());

		if (expectedState == ExecutionState.DONE) {
			assertThat(numberOfResults)
					.describedAs("Query results")
					.isEqualTo(size);
		}
	}

	private static URI getPostQueryURI(StandaloneSupport conquery) {
		return HierarchyHelper.fromHierachicalPathResourceMethod(conquery.defaultApiURIBuilder(), QueryResource.class, "postQuery")
							  .buildFromMap(Map.of(
									  "dataset", conquery.getDataset().getId()
							  ));
	}

	private static URI getQueryStatusURI(StandaloneSupport conquery, String id) {
		return HierarchyHelper.fromHierachicalPathResourceMethod(conquery.defaultApiURIBuilder(), QueryResource.class, "getStatus")
							  .buildFromMap(Map.of(
									  "query", id, "dataset", conquery.getDataset().getId()
							  ));
	}

}
