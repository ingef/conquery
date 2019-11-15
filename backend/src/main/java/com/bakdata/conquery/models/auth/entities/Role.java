package com.bakdata.conquery.models.auth.entities;

import java.util.Set;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
	protected synchronized void updateStorage(MasterMetaStorage storage) throws JSONException {
		storage.updateRole(this);
		
	}
}
