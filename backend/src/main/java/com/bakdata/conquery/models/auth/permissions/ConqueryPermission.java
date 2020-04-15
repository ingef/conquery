package com.bakdata.conquery.models.auth.permissions;

import java.time.Instant;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

/**
 * We wrap the actual interface to integrate the permissions into the 
 * {@link CPSType} system.
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface ConqueryPermission  extends Permission {
	
	/**
	 * Gets the domains of a permission according to {@link WildcardPermission}
	 * @return A set of the domains.
	 */
	Set<String> getDomains();
	
	/**
	 * Gets the abilities of a permission according to {@link WildcardPermission}
	 * @return A set of the abilities.
	 */
	Set<String> getAbilities();
	
	/**
	 * Gets the instances of a permission according to {@link WildcardPermission}
	 * @return A set of the instances.
	 */
	Set<String> getInstances();
	
	/**
	 * Returns the creation time of the permission.
	 * @return The creation time as an {@link Instant}.
	 */
	Instant getCreationTime();
	
	
}
