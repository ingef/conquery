package com.bakdata.conquery.models.auth;

import org.apache.shiro.realm.AuthorizingRealm;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

/**
 * Default configuration for the auth system. Sets up all other default components.
 *
 */
@CPSType(base = AuthConfig.class, id = "DEVELOPMENT")
public class DevAuthConfig extends AuthConfig {

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

	/**
	 * Handler for valid credentials that do not match any user.
	 */
	private static final UnknownUserHandler U_U_HANDLER = new DefaultUnknownUserHandler();

	
	@Getter
	@JsonIgnore
	private final TokenExtractor tokenExtractor = new DefaultTokenExtractor();

	@Override
	public AuthorizingRealm getRealm(MasterMetaStorage storage) {
		return new AllGrantedRealm();
	}

	@Override
	public UnknownUserHandler getUnknownUserHandler(MasterMetaStorage storage) {
		return U_U_HANDLER;
	}

	@Override
	public void initializeAuthConstellation(MasterMetaStorage storage) {
		try {
			storage.updateUser(USER);
		}
		catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}

}
