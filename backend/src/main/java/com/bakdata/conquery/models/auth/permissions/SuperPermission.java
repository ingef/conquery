package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.cps.CPSType;
import com.google.common.collect.ImmutableSet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id="SUPER_PERMISSION", base=ConqueryPermission.class)
public class SuperPermission extends ConqueryPermission {
	
	public final static String DUMMY_TARGET = "DUMMY_TARGET";
	public final static Set<Ability> ALLOWED_ABILITIES = ImmutableSet.of(Ability.DUMMY_ABILITY);
	

	public SuperPermission() {
		// Set a dummy ability
		super(Ability.DUMMY_ABILITY.asSet());
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

	@Override
	public Set<Ability> allowedAbilities() {
		return ALLOWED_ABILITIES;
	}

}
