package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

@CPSType(id = "DATASET", base = StringPermissionBuilder.class)
public class DatasetPermission extends StringPermissionBuilder {

	public static final String DOMAIN = "datasets";

	private static final Set<Ability> ALLOWED_ABILITIES = AbilitySets.DATASET_CREATOR;
	
	public static final DatasetPermission INSTANCE = new DatasetPermission();
	
	public ConqueryPermission instancePermission(Ability ability, DatasetId instance) {
		return instancePermission(ability, instance.toString());
	}
	
	public ConqueryPermission instancePermission(Set<Ability> abilities, DatasetId instance) {
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
	public static ConqueryPermission onInstance(Set<Ability> abilities, DatasetId instance) {
		return INSTANCE.instancePermission(abilities, instance);
	}

	@Deprecated
	public static ConqueryPermission onInstance(Ability ability, DatasetId instance) {
		return INSTANCE.instancePermission(ability, instance);
	}
}
