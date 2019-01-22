package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;
import java.util.UUID;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
public abstract class IdentifiableInstancePermission<ID extends AId<?>> extends ConqueryPermission {
	protected final ID instanceId;
	
	public IdentifiableInstancePermission(PermissionOwnerId<?> ownerId, Set<Ability> abilities,  ID instanceId) {
		super(ownerId, abilities);
		this.instanceId = instanceId;
	}
	
	@JsonCreator
	public IdentifiableInstancePermission(PermissionOwnerId<?> ownerId, Set<Ability> abilities,  ID instanceId, UUID jsonId) {
		super(ownerId, abilities, jsonId);
		this.instanceId = instanceId;
	}
	
	@Override
	public boolean implies(Permission permission) {
		// Check owner and accesses
		if(!super.implies(permission)) {
			return false;
		}
		
		// Check permission category
		if(!(permission instanceof IdentifiableInstancePermission)) {
			return false;
		}
		
		IdentifiableInstancePermission<?> ip = (IdentifiableInstancePermission<?>) permission;
		
		// Check instance
		return this.getInstanceId().equals(ip.getInstanceId());
	}

	@Override
	public abstract IdentifiableInstancePermission<ID> withOwner(PermissionOwnerId<?> newOwner);
	
	public ID getTarget() {
		return instanceId;
	}
}
