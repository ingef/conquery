package com.bakdata.conquery.models.auth.entities;

import java.util.Set;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Role extends PermissionOwner<RoleId> {


	public Role(String name, String label, MetaStorage storage) {
		super(name, label, storage);
	}

	@Override
	public Set<ConqueryPermission> getEffectivePermissions() {
		return getPermissions();
	}

	@Override
	protected void updateStorage() {
		getMetaStorage().updateRole(this);

	}

	@Override
	public RoleId createId() {
		RoleId roleId = new RoleId(name);
		roleId.setMetaStorage(getMetaStorage());
		return roleId;
	}

}
