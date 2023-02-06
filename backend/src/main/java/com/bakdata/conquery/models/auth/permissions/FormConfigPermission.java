package com.bakdata.conquery.models.auth.permissions;

import java.util.EnumSet;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;

@CPSType(id = "FORM_CONFIG", base = StringPermissionBuilder.class)
public class FormConfigPermission extends StringPermissionBuilder {

	public static final String DOMAIN = "form-config";
	

	public static final EnumSet<Ability> ALLOWED_ABILITIES = AbilitySets.FORM_CONFIG_CREATOR;
	
	public static final FormConfigPermission INSTANCE = new FormConfigPermission();
	
	
	private ConqueryPermission instancePermission(Ability ability, FormConfigId instance) {
		return instancePermission(ability, instance.toString());
	}

	private ConqueryPermission instancePermission(Set<Ability> abilities, FormConfigId instance) {
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
	@Deprecated
	public static ConqueryPermission onInstance(Set<Ability> abilities, FormConfigId instance) {
		return INSTANCE.instancePermission(abilities, instance);
	}
}
