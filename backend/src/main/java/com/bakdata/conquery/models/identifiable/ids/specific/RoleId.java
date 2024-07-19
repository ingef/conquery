package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
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
		components.add(TYPE);
		components.add(role);
	}

	@Override
	public void collectIds(Collection<? super Id<?>> collect) {
		collect.add(this);
	}

	enum Parser implements IdUtil.Parser<RoleId> {
		INSTANCE;

		@Override
		public RoleId parseInternally(IdIterator parts) {
			return (RoleId) PermissionOwnerId.Parser.INSTANCE.parse(parts);
		}
	}

	@Override
	public Role getPermissionOwner(MetaStorage storage) {
		return storage.getRole(this);
	}
}
