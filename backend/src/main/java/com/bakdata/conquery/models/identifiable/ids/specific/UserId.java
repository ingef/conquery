package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper=false)
public final class UserId extends PermissionOwnerId<User> {
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

	@Override
	public void collectIds(Collection<Id<?, ?>> collect) {
		collect.add(this);
	}

	@Override
	public User get() {
		return getDomain().getUser(this);
	}

	public enum Parser implements IdUtil.Parser<UserId> {
		INSTANCE;

		@Override
		public UserId parseInternally(IdIterator parts) {
			return (UserId) PermissionOwnerId.Parser.INSTANCE.parse(parts);
		}
	}

}
