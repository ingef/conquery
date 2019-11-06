package com.bakdata.conquery.models.auth.permissions;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.cps.CPSType;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Gives access to everything.
 *
 */
@Slf4j
@CPSType(id="SUPER_PERMISSION", base=ConqueryPermission.class)
@ToString(callSuper = true)
public final class SuperPermission extends SpecialPermission {	

	public SuperPermission() {
		super();
		log.info("Created SuperPermission");
	}
	
	/**
	 * Always grants the compared permission.
	 */
	@Override
	public boolean implies(Permission permission) {
		return true;
	}
}
