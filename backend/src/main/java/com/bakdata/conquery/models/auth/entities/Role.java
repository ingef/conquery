package com.bakdata.conquery.models.auth.entities;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

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
	protected void updateStorage(MasterMetaStorage storage) {
		storage.updateRole(this);
		
	}
}
