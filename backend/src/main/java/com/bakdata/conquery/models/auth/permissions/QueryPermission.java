package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.EqualsAndHashCode;

@CPSType(id="QUERY_PERMISSION", base=ConqueryPermission.class)
@EqualsAndHashCode(callSuper=true)
public class QueryPermission extends IdentifiableInstancePermission<ManagedExecutionId>{
	
	public QueryPermission(Set<Ability> abilities, ManagedExecutionId instanceId) {
		super(null, abilities, instanceId);
	}
	
	public QueryPermission(PermissionOwnerId<?> ownerId, Set<Ability> abilities, ManagedExecutionId instanceId) {
		super(ownerId, abilities, instanceId);
	}
	
	@JsonCreator
	public QueryPermission(PermissionOwnerId<?> ownerId, Set<Ability> abilities, ManagedExecutionId instanceId,  UUID jsonId) {
		super(ownerId, abilities, instanceId, jsonId);
	}

	@Override
	public IdentifiableInstancePermission<ManagedExecutionId> withOwner(PermissionOwnerId<?> newOwner) {
		return new QueryPermission(newOwner, this.getAbilities().clone(), this.getInstanceId());
	}

}
