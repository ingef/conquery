package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper=true)
public class UserId extends PermissionOwnerId<User> {
	public static final String TYPE = "user";

	@Getter
	private final String email;

	public UserId(String email) {
		super();
		this.email = email;
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		super.collectComponents(components);
		components.add(TYPE);
		components.add(email);
	}

	public enum Parser implements IId.Parser<UserId> {
		INSTANCE;
		
		@Override
		public UserId parseInternally(IdIterator parts) {
			return (UserId) PermissionOwnerId.Parser.INSTANCE.parse(parts);
		}
	}

	@Override
	public User getOwner(MasterMetaStorage storage) {
		return storage.getUser(this);
	}
}
