package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;

/**
 * Permission to restrict the usage of a specific form type.
 * The forms are programmatically distinguished from their CPSType.
 *
 */
@CPSType(id = "FORM", base = StringPermissionBuilder.class)
public class FormPermission extends StringPermissionBuilder {
	
	public static final String DOMAIN = "forms";

	private static final Set<Ability> ALLOWED_ABILITIES = Set.of(
		Ability.CREATE);
	
	public final static FormPermission INSTANCE = new FormPermission();

	@Override
	public String getDomain() {
		return DOMAIN;
	}

	@Override
	public Set<Ability> getAllowedAbilities() {
		return ALLOWED_ABILITIES;
	}

	//// Helper functions
	public static ConqueryPermission onInstance(Set<Ability> abilities, String instance) {
		return INSTANCE.instancePermission(abilities, instance);
	}

	public static ConqueryPermission onInstance(Ability ability, String instance) {
		return INSTANCE.instancePermission(ability, instance);
	}
}
