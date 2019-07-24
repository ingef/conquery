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
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class User extends PermissionOwner<UserId> implements Principal{
	@Getter @Setter @MetaIdRefCollection
	private Set<Mandator> roles = new HashSet<>();
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
		
		return isPermittedByMandators((ConqueryPermission)permission);
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
	
	public void removeRole(Mandator mandator) {
		roles.remove(mandator);
	}
	
	private boolean isPermittedByMandators(ConqueryPermission permission) {
		for(Mandator mandator : roles) {
			if(mandator.isPermittedSelfOnly(permission)) {
				return true;
			}
		}
		return false;
	}

	public void addMandator(MasterMetaStorage storage, Mandator mandator) throws JSONException {
		if(!roles.contains(mandator)) {
			addMandatorLocal(mandator);
			storage.updateUser(this);
		}
	}
	
	public void removeMandator(MasterMetaStorage storage, Mandator mandator) throws JSONException {
		if(roles.contains(mandator)) {
			roles.remove(mandator);
			storage.updateUser(this);
		}
	}

	public void addMandatorLocal(Mandator mandator) {
		roles.add(mandator);
	}

	@Override
	@JsonIgnore
	public String getName() {
		return email;
	}
	
	@Override
	public Set<ConqueryPermission> getPermissions(){
		Set<ConqueryPermission> permissions = new HashSet<>(super.getPermissions());
		for (Mandator mandator : roles) {
			permissions.addAll(mandator.getPermissions());
		}
		return permissions;
	}
}
