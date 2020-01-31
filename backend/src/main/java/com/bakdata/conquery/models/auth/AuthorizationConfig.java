package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import lombok.Getter;

/**
 * Conquery's authentication and authorization system uses this interface to retrieve necessary 
 * objects and other auth related informations for system for different configurations.
 * 
 * A custom authentication mechanism must implement this interface and register a JSON type from this interface,
 * before it is added to the base configuration {@link ConqueryConfig}.
 */
public class AuthorizationConfig {


	/**
	 * The label of the superuser that is used in the frontend.
	 */
	private static final String LABEL = "admin";
	
	/**
	 * The email of the superuser that is used in the frontend.
	 */
	private static final String EMAIL = "admin";
	
	/**
	 * 
	 */
	private static final String PASSWORD = "admin";

	/**
	 * The superuser.
	 */
	public static final User USER = new User(EMAIL, LABEL);

	/**
	 * Returns an ordered list of Permission scopes that are used to generate an permission overview for a user (in {@link AdminProcessor}).
	 * @return A list of permission scopes.
	 */
	@Getter
	public List<String> overviewScope = List.of(
		DatasetPermission.DOMAIN,
		AdminPermission.DOMAIN,
		SuperPermission.DOMAIN);
	
	/**
	 * Sets up the initial subjects and permissions for the authentication system.
	 * @param storage A storage, where the handler might add a new users.
	 */
	public void initializeAuthConstellation(MasterMetaStorage storage, AuthorizationController controller) {
		try {
			storage.updateUser(USER);
			USER.addPermission(storage, SuperPermission.onDomain());
			for(ConqueryRealm realm : controller.getRealms()) {
				if(realm instanceof UserManageable) {
					((UserManageable) realm).addUser(USER.getId().getEmail(), PASSWORD, true);
				}
			}
		}
		catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}
}
