package com.bakdata.conquery.models.auth.subjects;

import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.jackson.serializer.IdReferenceCollection;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

public class User extends PermissionOwner<UserId> implements Principal{
	@Getter @Setter
	@IdReferenceCollection
	private Set<Mandator> roles = new HashSet<>();


	public User(UserId id) {
		super(new SinglePrincipalCollection(id));
	}
	
	@JsonCreator
	public User(SinglePrincipalCollection principals) {
		super(principals);
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
		boolean ret[] = new boolean[permissions.size()];
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
		return (UserId) getPrincipals().getPrimaryPrincipal();
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

	public void addMandator(Mandator mandator) throws JSONException {
		if(!roles.contains(mandator)) {
			addMandatorLocal(mandator);
			this.getStorage().updateUser(this);
		}
	}
	
	public void removeMandatorLocal(Mandator mandator) throws JSONException {
		if(roles.contains(mandator)) {
			roles.remove(mandator);
			this.getStorage().updateUser(this);
		}
	}

	public void addMandatorLocal(Mandator mandator) {
		roles.add(mandator);
	}
}
