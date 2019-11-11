package com.bakdata.conquery.models.auth.subjects;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;

import com.bakdata.conquery.io.jackson.serializer.MetaIdRefCollection;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class User extends FilteredUser<UserId> implements Principal{
	@Getter @Setter @MetaIdRefCollection
	private Set<Role> roles = Collections.synchronizedSet( new HashSet<>());
	@Getter @Setter @NonNull @NotNull
	private String email;
	@Getter @Setter @NonNull @NotNull
	private String label;

	public User(String email, String label) {
		this.email = email;
		this.label = label;
	}
	


	@Override
	public void checkPermission(Permission permission) throws AuthorizationException {
		SecurityUtils.getSecurityManager().checkPermission(getPrincipals(), permission);
	}

	@Override
	public void checkPermissions(Collection<Permission> permissions) throws AuthorizationException {
		SecurityUtils.getSecurityManager().checkPermissions(getPrincipals(), permissions);
	}

	@Override
	public PrincipalCollection getPrincipals() {
		return new SinglePrincipalCollection(getId());
	}
	
	@Override
	public boolean isPermitted(Permission permission) {
		return SecurityUtils.getSecurityManager().isPermitted(getPrincipals(), permission);
	}

	@Override
	public boolean[] isPermitted(List<Permission> permissions) {
		return SecurityUtils.getSecurityManager().isPermitted(getPrincipals(), permissions);
	}

	@Override
	public boolean isPermittedAll(Collection<Permission> permissions) {
		return SecurityUtils.getSecurityManager().isPermittedAll(getPrincipals(), permissions);
	}
	
	@Override
	public UserId createId() {
		return new UserId(email);
	}

	public void addRole(MasterMetaStorage storage, Role role) throws JSONException {
		synchronized (roles) {
			if(!roles.contains(role)) {
				addRoleLocal(role);
				updateStorage(storage);
			}
		}
	}
	
	public void removeRole(MasterMetaStorage storage, Role role) throws JSONException {
		synchronized (roles) {
			if(roles.contains(role)) {
				roles.remove(role);
				updateStorage(storage);
			}
		}
	}

	/**
	 * At role to the local role set only.
	 * @param mandator
	 */
	public void addRoleLocal(Role mandator) {
		roles.add(mandator);
	}

	@Override
	@JsonIgnore
	public String getName() {
		return email;
	}
	
	@JsonIgnore
	public Set<ConqueryPermission> getEffectivePermissions(){
		Set<ConqueryPermission> permissions = copyPermissions();
		for (Role role : roles) {
			permissions.addAll(role.getEffectivePermissions());
		}
		return permissions;
	}
	
	@Override
	protected synchronized void updateStorage(MasterMetaStorage storage) throws JSONException {
		storage.updateUser(this);
	}



	@Override
	public Object getPrincipal() {
		return getId();
	}
}
