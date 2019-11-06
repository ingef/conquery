package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper=false)
public class RoleId extends PermissionOwnerId<Role> {
	public static final String TYPE = "role";
	
	@Getter
	private final String role;
	
	public RoleId(String mandator) {
		super();
		this.role = mandator;
	}

	public void collectComponents(List<Object> components) {
		super.collectComponents(components);
		components.add(TYPE);
		components.add(role);
	}
	
	enum Parser implements IId.Parser<RoleId> {
		INSTANCE;
		
		@Override
		public RoleId parseInternally(IdIterator parts) {
			return (RoleId) PermissionOwnerId.Parser.INSTANCE.parse(parts);
		}
	}

	@Override
	public Role getOwner(MasterMetaStorage storage) {
		return storage.getRole(this);
	}
}
