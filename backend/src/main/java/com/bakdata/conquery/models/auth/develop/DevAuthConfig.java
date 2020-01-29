package com.bakdata.conquery.models.auth.develop;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthConfig;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryRealm;
import com.bakdata.conquery.models.auth.UserManageable;
import com.bakdata.conquery.models.auth.basic.BasicAuthRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
	
	@JsonIgnore
	private List<ConqueryRealm> realms = null;
	
	@Getter
	public final List<String> overviewScope = List.of(
		DatasetPermission.DOMAIN,
		AdminPermission.DOMAIN,
		SuperPermission.DOMAIN);

	public List<ConqueryRealm> getRealms(MasterMetaStorage storage){
		if(realms == null) {
			realms = List.of(new AllGrantedRealm(), new BasicAuthRealm(storage));
		}
		return realms;
	}
	

	@Override
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
