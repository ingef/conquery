package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.shiro.realm.AuthorizingRealm;

/**
 * Conquery's authentication and authorization system uses this interface to retrieve necessary 
 * objects and other auth related informations for system for different configurations.
 * 
 * A custom authentication mechanism must implement this interface and register a JSON type from this interface,
 * before it is added to the base configuration {@link ConqueryConfig}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface AuthConfig {
	/**
	 * Gets the realm to be uses in the current configuration.
	 * @param storage A storage from which a realm can query information about subjects and permissions.
	 * @return The realm.
	 */
	AuthorizingRealm getRealm(MasterMetaStorage storage);
	
	/**
	 * Sets up the initial subjects and permissions for the authentication system.
	 * @param storage A storage, where the handler might add a new users.
	 */
	void initializeAuthConstellation(MasterMetaStorage storage);
	
	/**
	 * Gets an extractor that parse a token from a request.
	 * The token is then used in the authentication process.
	 * @return The extractor
	 */
	TokenExtractor getTokenExtractor();
	
	/**
	 * Returns an ordered list of Permission scopes that are used to generate an permission overview for a user (in {@link AdminProcessor}).
	 * @return A list of permission scopes.
	 */
	List<String> getOverviewScope();
}
