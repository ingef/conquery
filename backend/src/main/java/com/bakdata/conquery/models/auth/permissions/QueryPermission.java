package com.bakdata.conquery.models.auth.permissions;

import java.util.EnumSet;
import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id="QUERY_PERMISSION", base=ConqueryPermission.class)
public class QueryPermission extends IdentifiableInstancePermission<ManagedQueryId>{

	public QueryPermission(PermissionOwnerId<?> ownerId, EnumSet<Ability> abilities, ManagedQueryId instanceId) {
		super(ownerId, abilities, instanceId);
	}
	
	@JsonCreator
	public QueryPermission(PermissionOwnerId<?> ownerId, EnumSet<Ability> abilities, ManagedQueryId instanceId,  UUID jsonId) {
		super(ownerId, abilities, instanceId, jsonId);
	}

	@Override
	public IdentifiableInstancePermission<ManagedQueryId> withOwner(PermissionOwnerId<?> newOwner) {
		return new QueryPermission(newOwner, this.getAbilities().clone(), this.getInstanceId());
	}

}
