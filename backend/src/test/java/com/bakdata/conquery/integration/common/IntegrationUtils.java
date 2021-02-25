package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
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
		DatasetId dataset = conquery.getNamespace().getDataset().getId();

		final URI postQueryURI = getPostQueryURI(conquery, dataset);

		Response response = conquery.getClient().target(postQueryURI)
									.request(MediaType.APPLICATION_JSON_TYPE)
									.post(Entity.entity(query, MediaType.APPLICATION_JSON_TYPE));

		if (response.getStatusInfo().getFamily().equals(Response.Status.Family.familyOf(400)) && expectedState.equals(ExecutionState.FAILED)) {
			return;
		}

		ExecutionStatus status = response.readEntity(ExecutionStatus.Full.class);

		final URI queryStatusURI = getQueryStatusURI(conquery, status);

		// We try at most 5 times, queryStatus waits for 10s, we therefore don't need to timeout here.
		// Query getQueryStatus until it is no longer running.
		for (int trial = 0; trial < 5; trial++) {

			final ExecutionStatus currentStatus =
					conquery.getClient()
							.target(queryStatusURI)
							.request(MediaType.APPLICATION_JSON_TYPE)
							.get(ExecutionStatus.Full.class);

			if (currentStatus.getStatus() != ExecutionState.RUNNING) {
				status = currentStatus;
				break;
			}
		}

		assertThat(status.getStatus()).isEqualTo(expectedState);

		if (expectedState == ExecutionState.DONE) {
			assertThat(status.getNumberOfResults())
					.describedAs("Query results")
					.isEqualTo(size);
		}
	}

	private static URI getPostQueryURI(StandaloneSupport conquery, DatasetId dataset) {
		return HierarchyHelper.fromHierachicalPathResourceMethod(UriBuilder.fromPath("api")
																		   .host("localhost")
																		   .scheme("http")
																		   .port(conquery.getLocalPort()), QueryResource.class, "postQuery")
							  .buildFromMap(Map.of(
									  "dataset", conquery.getDataset().getId()
							  ));
	}

	private static URI getQueryStatusURI(StandaloneSupport conquery, ExecutionStatus status) {
		return HierarchyHelper.fromHierachicalPathResourceMethod(UriBuilder.fromPath("api")
																		   .host("localhost")
																		   .scheme("http")
																		   .port(conquery.getLocalPort()), QueryResource.class, "getStatus")
							  .buildFromMap(Map.of(
									  "query", status.getId(), "dataset", conquery.getDataset().getId()
							  ));
	}
}
