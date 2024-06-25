package com.bakdata.conquery.models.auth.entities;

import java.util.Set;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Role extends PermissionOwner<RoleId> {


	public Role(String name, String label) {
		super(name, label);
	}

	@Override
	public Set<ConqueryPermission> getEffectivePermissions() {
		return getPermissions();
	}

	@Override
	public RoleId createId() {
		return new RoleId(name);
	}

	@Override
	protected void updateStorage() {
		getMetaIdResolver().updateRole(this);

	}

}
