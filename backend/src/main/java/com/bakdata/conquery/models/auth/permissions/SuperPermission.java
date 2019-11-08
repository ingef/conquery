package com.bakdata.conquery.models.auth.permissions;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonIgnore;

@CPSType(id="SUPER_PERMISSION", base=ConqueryPermission.class)
public class SuperPermission extends ConqueryPermission {
	
	private static final String DUMMY_TARGET = "dummy";
	
	public SuperPermission() {
		// Add a dummy ability here for now. This class becomes obsolete with introduction of the role system.
		super(Ability.READ.asSet());
	}

	@Override
	public boolean implies(Permission permission) {
		
		return true;
	}
	
	@JsonIgnore
	@Override
	public Object getTarget() {
		return DUMMY_TARGET;
	}

}
