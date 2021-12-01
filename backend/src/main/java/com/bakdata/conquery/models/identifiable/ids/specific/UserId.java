package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper=false)
public class UserId extends PermissionOwnerId<User> {
	public static final String TYPE = "user";

	@Getter
	private final String name;

	public UserId(String name) {
		super();
		this.name = name;
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		components.add(TYPE);
		components.add(name);
	}

	public enum Parser implements IId.Parser<UserId> {
		INSTANCE;
		
		@Override
		public UserId parseInternally(IdIterator parts) {
			return (UserId) PermissionOwnerId.Parser.INSTANCE.parse(parts);
		}
	}

	@Override
	public User getPermissionOwner(MetaStorage storage) {
		return storage.getUser(this);
	}
}
