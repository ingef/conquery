package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.shiro.realm.AuthorizingRealm;

/**
 * This interface represents the basic modules that conquery's authentication and authorization system needs.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public abstract class AuthConfig {
	/**
	 * Gets the realm specified in the configuration.
	 * @param storage (Unused) A storage from which a realm can query information about subjects and permissions.
	 * @return The realm.
	 */
	public abstract AuthorizingRealm getRealm(MasterMetaStorage storage);
	
	/**
	 * Sets up the initial subjects and permissions for the authentication system.
	 * @param storage A storage, where the handler might add a new users.
	 */
	public abstract void initializeAuthConstellation(MasterMetaStorage storage);
	
	/**
	 * Gets an extractor that parse a token from a request.
	 * The token is then used in the authentication process.
	 * @return The extractor
	 */
	public abstract TokenExtractor getTokenExtractor();
	
	/**
	 * Returns an ordered list of Permission scopes that are used to generate an permission overview for a user.
	 * @return
	 */
	public abstract List<String> getOverviewScope();
}
