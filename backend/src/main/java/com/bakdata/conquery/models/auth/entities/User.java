package com.bakdata.conquery.models.auth.entities;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;

@Slf4j
public class User extends FilteredUser<UserId> implements Principal, RoleOwner {

	@JsonProperty
	private Set<RoleId> roles = Collections.synchronizedSet( new HashSet<>());

	@Getter @Setter @JsonIgnore
	private transient boolean displayLogout = true;

	public User(String name, String label) {
		super(name, label);
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

	public void addRole(MetaStorage storage, Role role) {
		if(roles.add(role.getId())) {
			log.trace("Added role {} to user {}", role.getId(), getId());
			updateStorage(storage);
		}
	}
	
	@Override
	public void removeRole(MetaStorage storage, Role role) {
		if(roles.remove(role.getId())) {
			log.trace("Removed role {} from user {}", role.getId(), getId());				
			updateStorage(storage);
		}
	}

	public Set<RoleId> getRoles(){
		return Collections.unmodifiableSet(roles);
	}
	
	@Override
	protected void updateStorage(MetaStorage storage) {
		storage.updateUser(this);
	}



	@Override
	public Object getPrincipal() {
		return getId();
	}
}
