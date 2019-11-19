package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Gives access to everything.
 *
 */
@Slf4j
@ToString(callSuper = true)
@CPSType(id = "SUPER", base = StringPermissionBuilder.class)
public final class SuperPermission extends StringPermissionBuilder {	


	private static final String DOMAIN = "*";
	
	public final static SuperPermission INSTANCE = new SuperPermission();

	@Override
	public String getDomain() {
		return DOMAIN;
	}

	@Override
	public Set<Ability> getAllowedAbilities() {
		return Set.of();
	}
	
	public static ConqueryPermission onDomain() {
		return INSTANCE.domainPermission();
	}
}
