package com.bakdata.conquery.models.auth.subjects;

import java.util.Collection;
import java.util.List;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.models.identifiable.ids.specific.NullSubjectId;

public class NullSubject extends PermissionOwner<NullSubjectId> {

	public final static NullSubject INSTANCE = new NullSubject();
	
	private NullSubject() {
		super();
	}

	@Override
	public boolean isPermitted(Permission permission) {
		return false;
	}

	@Override
	public boolean[] isPermitted(List<Permission> permissions) {
		return new boolean[] {};
	}

	@Override
	public boolean isPermittedAll(Collection<Permission> permissions) {
		return false;
	}

	@Override
	public void checkPermission(Permission permission) throws AuthorizationException {
		throw new AuthorizationException("NullSubject is not permitted to perform any action");
	}

	@Override
	public void checkPermissions(Collection<Permission> permissions) throws AuthorizationException {
		throw new AuthorizationException("NullSubject is not permitted to perform any action");
	}

	@Override
	public NullSubjectId createId() {
		return new NullSubjectId();
	}

}
