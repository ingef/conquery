package com.bakdata.conquery.models.auth.subjects;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.fasterxml.jackson.annotation.JsonCreator;

public class Mandator extends PermissionOwner<MandatorId> {
	
	@JsonCreator
	public Mandator(SinglePrincipalCollection principals) {
		super(principals);
	}

	@Override
	public MandatorId createId() {
		return (MandatorId) getPrincipals().getPrimaryPrincipal();
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
