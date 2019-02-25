package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.EqualsAndHashCode;

@CPSType(id="QUERY_PERMISSION", base=ConqueryPermission.class)
@EqualsAndHashCode(callSuper=true)
public class QueryPermission extends IdentifiableInstancePermission<ManagedQueryId>{
	
	public QueryPermission(Set<Ability> abilities, ManagedQueryId instanceId) {
		super(null, abilities, instanceId);
	}
	
	public QueryPermission(PermissionOwnerId<?> ownerId, Set<Ability> abilities, ManagedQueryId instanceId) {
		super(ownerId, abilities, instanceId);
	}
	
	@JsonCreator
	public QueryPermission(PermissionOwnerId<?> ownerId, Set<Ability> abilities, ManagedQueryId instanceId,  UUID jsonId) {
		super(ownerId, abilities, instanceId, jsonId);
	}

	@Override
	public IdentifiableInstancePermission<ManagedQueryId> withOwner(PermissionOwnerId<?> newOwner) {
		return new QueryPermission(newOwner, this.getAbilities().clone(), this.getInstanceId());
	}

}
