package com.bakdata.conquery.integration.common;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;

import lombok.experimental.UtilityClass;

@UtilityClass
public class IntegrationUtils {


	/**
	 * Load the constellation of roles, users and permissions into the provided storage.
	 */
	public static void importPermissionConstellation(MasterMetaStorage storage,
			Role [] roles,
			RequiredUser [] rUsers) throws JSONException {
				
		for(Role role: roles) {
			storage.addRole(role);
		}
		
		for(RequiredUser rUser: rUsers) {
			User user = rUser.getUser();
			storage.addUser(user);

			RoleId [] rolesInjected = rUser.getRolesInjected();
			
			for(RoleId mandatorId : rolesInjected) {
				user.addRole(storage, storage.getRole(mandatorId));
			}
		}
	}
	


	public static void clearAuthStorage(MasterMetaStorage storage) {
		// Clear MasterStorage
		for(Role mandator : storage.getAllRoles()) {
			storage.removeRole(mandator.getId());
		}
		for(User user : storage.getAllUsers()) {
			storage.removeUser(user.getId());
		}
	}
}
