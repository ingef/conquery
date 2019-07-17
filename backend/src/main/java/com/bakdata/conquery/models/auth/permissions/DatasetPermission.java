package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.EqualsAndHashCode;

@CPSType(id="DATASET_PERMISSION", base=ConqueryPermission.class)
@EqualsAndHashCode(callSuper=true)
public class DatasetPermission extends IdentifiableInstancePermission<DatasetId> {

	public DatasetPermission(PermissionOwnerId<?> ownerId, Set<Ability> abilities, DatasetId instanceId) {
		super(ownerId, abilities, instanceId);
	}
	
	public DatasetPermission(PermissionOwner<?> owner, Set<Ability> abilities, DatasetId instanceId) {
		super(owner.getId(), abilities, instanceId);
	}
	
	public DatasetPermission(Set<Ability> abilities, DatasetId instanceId) {
		super(null, abilities, instanceId);
	}

	@JsonCreator
	public DatasetPermission(PermissionOwnerId<?> ownerId, Set<Ability> abilities, DatasetId instanceId, UUID jsonId) {
		super(ownerId, abilities, instanceId, jsonId);
	}

	@Override
	public DatasetPermission withOwner(PermissionOwnerId<?> newOwner) {
		return new DatasetPermission(newOwner, this.getAbilities().clone(), this.getInstanceId());
	}
}
