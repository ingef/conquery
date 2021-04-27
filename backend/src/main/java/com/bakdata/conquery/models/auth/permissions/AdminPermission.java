package com.bakdata.conquery.models.auth.permissions;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * For granting access to the admin servlet.
 *
 */
@Slf4j
@ToString(callSuper = true)
@CPSType(id = "ADMIN", base = StringPermissionBuilder.class)
public final class AdminPermission extends StringPermissionBuilder {

	public static final String DOMAIN = "admin";

	public static final AdminPermission INSTANCE = new AdminPermission();

	@Override
	public String getDomain() {
		return DOMAIN;
	}

	/*
	 * Can be further specialized if needed.
	 */
	@Override
	public Set<Ability> getAllowedAbilities() {
		return Set.of(Ability.READ);
	}

	public static ConqueryPermission onDomain() {
		return INSTANCE.domainPermission();
	}

	public static ConqueryPermission onDomain(Set<Ability> abilities) {
		return INSTANCE.abilityPermission(abilities);
	}
}
