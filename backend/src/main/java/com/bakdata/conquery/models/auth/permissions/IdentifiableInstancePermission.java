package com.bakdata.conquery.models.auth.permissions;

import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.models.identifiable.ids.AId;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
public abstract class IdentifiableInstancePermission<ID extends AId<?>> extends ConqueryPermission {
	protected final ID instanceId;
	
	public IdentifiableInstancePermission(Set<Ability> abilities,  ID instanceId) {
		super(abilities);
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

	public ID getTarget() {
		return instanceId;
	}
}
