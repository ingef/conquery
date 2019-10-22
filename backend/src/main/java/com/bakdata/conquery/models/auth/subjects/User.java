package com.bakdata.conquery.models.auth.subjects;

import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.jackson.serializer.MetaIdRefCollection;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class User extends PermissionOwner<UserId> implements Principal{
	@Getter @Setter @MetaIdRefCollection
	private Set<Role> roles = new HashSet<>();
	@Getter @Setter @NonNull @NotNull
	private String email;
	@Getter @Setter @NonNull @NotNull
	private String label;

	public User(String email, String label) {
		this.email = email;
		this.label = label;
	}
	
	@Override
	public boolean isPermitted(Permission permission) {
		if(isPermittedSelfOnly((ConqueryPermission)permission)) {
			return true;
		}
		
		return isPermittedByRoles((ConqueryPermission)permission);
	}

	@Override
	public boolean[] isPermitted(List<Permission> permissions) {
		boolean[] ret = new boolean[permissions.size()];
		for(int i = 0; i < permissions.size(); ++i) {
			ret[i] = isPermitted(permissions.get(i));
		}
		return ret;
	}

	@Override
	public boolean isPermittedAll(Collection<Permission> permissions) {
		for(Permission permission : permissions) {
			if(!isPermitted(permission)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public UserId createId() {
		return new UserId(email);
	}
	
	private boolean isPermittedByRoles(ConqueryPermission permission) {
		for(Role mandator : roles) {
			if(mandator.isPermittedSelfOnly(permission)) {
				return true;
			}
		}
		return false;
	}

	public void addRole(MasterMetaStorage storage, Role role) throws JSONException {
		synchronized (roles) {
			if(!roles.contains(role)) {
				addMandatorLocal(role);
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
	 * Only to be called from a context that is synchronized on `roles`.
	 * @param mandator
	 */
	public void addMandatorLocal(Role mandator) {
		roles.add(mandator);
	}

	@Override
	@JsonIgnore
	public String getName() {
		return email;
	}
	
	@JsonIgnore
	public Set<ConqueryPermission> getPermissionsEffective(){
		Set<ConqueryPermission> permissions = getPermissionsCopy();
		for (Role role : roles) {
			permissions.addAll(role.getPermissionsEffective());
		}
		return permissions;
	}
	
	@Override
	protected void updateStorage(MasterMetaStorage storage) throws JSONException {
		storage.updateUser(this);
	}
}
