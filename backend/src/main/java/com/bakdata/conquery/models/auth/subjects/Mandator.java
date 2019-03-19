package com.bakdata.conquery.models.auth.subjects;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@AllArgsConstructor
public class Mandator extends PermissionOwner<MandatorId> {

	@Getter @Setter @NonNull @NotNull
	private String name;
	@Getter @Setter @NonNull @NotNull
	private String label;
	

	@Override
	public MandatorId createId() {
		return new MandatorId(name);
	}

	@Override
	public boolean isPermitted(Permission permission) {
		return  isPermittedSelfOnly((ConqueryPermission)permission);
	}

	@Override
	public boolean[] isPermitted(List<Permission> permissions) {
		List<Permission> mandatorPermission = permissions.stream()
				.map(p -> ((ConqueryPermission)p).withOwner(this.getId()))
				.collect(Collectors.toList());
		return  SecurityUtils.getSecurityManager().isPermitted(getPrincipals(), mandatorPermission);
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
}
