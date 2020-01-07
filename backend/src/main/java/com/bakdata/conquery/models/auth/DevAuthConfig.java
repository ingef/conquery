package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.apache.shiro.realm.AuthorizingRealm;

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
	
	@Getter
	public final List<String> OverviewScope = List.of(
		DatasetPermission.DOMAIN,
		AdminPermission.DOMAIN,
		SuperPermission.DOMAIN);

	
	@Getter
	@JsonIgnore
	private final TokenExtractor tokenExtractor = new DefaultTokenExtractor();

	@Override
	public AuthorizingRealm getRealm(MasterMetaStorage storage) {
		return new AllGrantedRealm(storage);
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
