package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper=false)
public final class GroupId extends PermissionOwnerId<Group> {

	public static final String TYPE = "group";
	
	private final String group;
	
	public GroupId(String group) {
		super();
		this.group = group;
	}

	public void collectComponents(List<Object> components) {
		components.add(TYPE);
		components.add(group);
	}

	@Override
	public void collectIds(Collection<Id<?, ?>> collect) {
		collect.add(this);
	}

	@Override
	public Group get() {
		return getDomain().getGroup(this);
	}

	public enum Parser implements IdUtil.Parser<GroupId> {
		INSTANCE;

		@Override
		public GroupId parseInternally(IdIterator parts) {
			return (GroupId) PermissionOwnerId.Parser.INSTANCE.parse(parts);
		}
	}

}
