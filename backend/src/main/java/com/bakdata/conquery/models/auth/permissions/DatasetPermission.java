package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

@CPSType(id = "DATASETS", base = StringPermission.class)
public class DatasetPermission extends StringPermission {

	private static final String DOMAIN = "datasets";

	private static final Set<Ability> ALLOWED_ABILITIES = AbilitySets.DATASET_CREATOR;
	
	public final static DatasetPermission INSTANCE = new DatasetPermission();
	
	public PermissionMixin instancePermission(Ability ability, DatasetId instance) {
		return instancePermission(ability, instance.toString());
	}
	
	public PermissionMixin instancePermission(Set<Ability> abilities, DatasetId instance) {
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
}
