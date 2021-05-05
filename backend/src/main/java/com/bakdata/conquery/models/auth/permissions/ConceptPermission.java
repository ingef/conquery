package com.bakdata.conquery.models.auth.permissions;

import java.util.EnumSet;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;

@CPSType(id = "CONCEPT", base = StringPermissionBuilder.class)
public class ConceptPermission extends StringPermissionBuilder {

	public static final String DOMAIN = "concepts";
	

	public static final EnumSet<Ability> ALLOWED_ABILITIES = EnumSet.of(
		Ability.READ
	);
	
	public static final ConceptPermission INSTANCE = new ConceptPermission();
	
	
	private ConqueryPermission instancePermission(Ability ability, ConceptId instance) {
		return instancePermission(ability, instance.toString());
	}

	private ConqueryPermission instancePermission(Set<Ability> abilities, ConceptId instance) {
		return instancePermission(abilities, instance.toString());
	}

	@Override
	public String getDomain() {
		return DOMAIN;
	}

	@Override
	public Set<Ability> getAllowedAbilities() {
		return ALLOWED_ABILITIES;
	}

	//// Helper functions

	public static ConqueryPermission onInstance(Set<Ability> abilities, ConceptId instance) {
		return INSTANCE.instancePermission(abilities, instance);
	}
}
