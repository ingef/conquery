package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.cps.CPSType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id="SUPER_PERMISSION", base=ConqueryPermission.class)
public class SuperPermission extends ConqueryPermission {
	
	public final static Object DUMMY_TARGET = new Object();	
	public final static Set<Ability> DUMMY_ABILITY = Ability.READ.asSet();
	

	public SuperPermission() {
		// Set a dummy ability
		super(DUMMY_ABILITY);
		log.info("Created SuperPermission");
	}
	
	@Override
	public boolean implies(Permission permission) {
		return true;
	}

	@Override
	public Object getTarget() {
		return DUMMY_TARGET;
	}

}
