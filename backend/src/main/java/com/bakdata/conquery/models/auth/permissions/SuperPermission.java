package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Gives access to everything.
 *
 */
@Slf4j
@ToString(callSuper = true)
public final class SuperPermission extends StringPermissionBuilder {

	public static final String DOMAIN = "*";

	public static final SuperPermission INSTANCE = new SuperPermission();

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
