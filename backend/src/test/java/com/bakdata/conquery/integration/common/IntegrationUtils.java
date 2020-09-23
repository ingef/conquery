package com.bakdata.conquery.integration.common;

import java.io.IOException;

import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.query.IQuery;
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
}
