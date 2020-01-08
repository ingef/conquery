package com.bakdata.conquery.models.auth.entities;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.MetaIdRefCollection;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;

@Slf4j
public class User extends FilteredUser<UserId> implements Principal, RoleOwner {

	@MetaIdRefCollection
	private Set<Role> roles = Collections.synchronizedSet( new HashSet<>());
	
	/**
	 * The name of a user is unique in the system. It is used to generate the UserId object.
	 * The name might be an email address.
	 */
	@Getter @Setter @NonNull @NotNull
	private String name;
	@Getter @Setter @NonNull @NotNull
	private String label;

	public User(String name, String label) {
		this.name = name;
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
		return new UserId(name);
	}

	public void addRole(MasterMetaStorage storage, Role role) throws JSONException {
		if(roles.add(role)) {
			log.trace("Added role {} to user {}", role.getId(), getId());
			updateStorage(storage);
		}
	}
	
	@Override
	public void removeRole(MasterMetaStorage storage, Role role) throws JSONException {
		if(roles.remove(role)) {
			log.trace("Removed role {} from user {}", role.getId(), getId());				
			updateStorage(storage);
		}
	}

	public Set<Role> getRoles(){
		return Set.copyOf(roles);
	}
	
	@Override
	protected void updateStorage(MasterMetaStorage storage) throws JSONException {
		storage.updateUser(this);
	}



	@Override
	public Object getPrincipal() {
		return getId();
	}
}
