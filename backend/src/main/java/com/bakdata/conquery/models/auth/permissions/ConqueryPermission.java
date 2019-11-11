package com.bakdata.conquery.models.auth.permissions;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface ConqueryPermission  extends Permission{
	
}
