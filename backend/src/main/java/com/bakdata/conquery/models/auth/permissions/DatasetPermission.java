package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.google.common.collect.ImmutableSet;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@CPSType(id="DATASET_PERMISSION", base=ConqueryPermission.class)
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class DatasetPermission extends IdentifiableInstancePermission<DatasetId> {
	
	public final static Set<Ability> ALLOWED_ABILITIES = ImmutableSet.copyOf(AbilitySets.DATASET_CREATOR);
	
	public DatasetPermission(Set<Ability> abilities, DatasetId instanceId) {
		super(abilities, instanceId);
	}

	@Override
	public Set<Ability> allowedAbilities() {
		return ALLOWED_ABILITIES;
	}
}
