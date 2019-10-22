package com.bakdata.conquery.models.auth.subjects;

import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@AllArgsConstructor
public class Role extends PermissionOwner<RoleId> {

	@Getter @Setter @NonNull @NotNull @NotEmpty
	private String name;
	@Getter @Setter @NonNull @NotNull @NotEmpty
	private String label;
	

	@Override
	public RoleId createId() {
		return new RoleId(name);
	}

	@Override
	public boolean isPermitted(Permission permission) {
		return  isPermittedSelfOnly((ConqueryPermission)permission);
	}

	@Override
	public boolean[] isPermitted(List<Permission> permissions) {
		return  SecurityUtils.getSecurityManager().isPermitted(getPrincipals(), permissions);
	}

	@Override
	public boolean isPermittedAll(Collection<Permission> permissions) {
		long numberPermitted = permissions.stream().filter(p -> SecurityUtils.getSecurityManager().isPermitted(getPrincipals(), p)).count();
		return numberPermitted == permissions.size();
	}

	@Override
	public void checkPermission(Permission permission) throws AuthorizationException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void checkPermissions(Collection<Permission> permissions) throws AuthorizationException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isAuthenticated() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRemembered() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void updateStorage(MasterMetaStorage storage) throws JSONException {
		storage.updateRole(this);
		
	}
}
