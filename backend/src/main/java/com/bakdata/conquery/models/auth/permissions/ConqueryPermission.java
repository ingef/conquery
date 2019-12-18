package com.bakdata.conquery.models.auth.permissions;

import java.time.Instant;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.shiro.authz.Permission;

/**
 * We wrap the actual interface to integrate the permissions into the 
 * CPSType system.
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface ConqueryPermission  extends Permission{
	
	Set<String> getDomains();
	Set<String> getAbilities();
	Set<String> getInstances();
	Instant getCreationTime();
	
	
}
