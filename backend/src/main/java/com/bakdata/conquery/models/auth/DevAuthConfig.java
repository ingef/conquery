package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import lombok.Getter;

/**
 * Default configuration for the auth system. Sets up all other default components.
 * This configuration causes that every request is handled as invoked by the super user.
 */
@CPSType(base = AuthConfig.class, id = "DEVELOPMENT")
public class DevAuthConfig implements AuthConfig {

	/**
	 * The label of the superuser that is used in the frontend.
	 */
	private static final String LABEL = "SUPERUSER";
	
	/**
	 * The email of the superuser that is used in the frontend.
	 */
	private static final String EMAIL = "SUPERUSER@SUPERUSER";

	/**
	 * The superuser.
	 */
	public static final User USER = new User(EMAIL, LABEL);
	
	@Getter
	public final List<String> overviewScope = List.of(
		DatasetPermission.DOMAIN,
		AdminPermission.DOMAIN,
		SuperPermission.DOMAIN);

	
	private ConqueryRealm realm = new AllGrantedRealm();

	@Override
	public List<ConqueryRealm> getRealms() {
		return List.of(realm);
	}

	@Override
	public void initializeAuthConstellation(AuthorizationStorage storage) {
		try {
			storage.updateUser(USER);
			USER.addPermission(storage, SuperPermission.onDomain());
		}
		catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}
}
