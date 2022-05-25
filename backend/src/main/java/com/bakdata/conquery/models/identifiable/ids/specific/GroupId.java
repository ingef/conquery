package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper=false)
public class GroupId extends PermissionOwnerId<Group> {

	public static final String TYPE = "group";
	
	@Getter
	private final String group;
	
	public GroupId(String group) {
		super();
		this.group = group;
	}

	public void collectComponents(List<Object> components) {
		components.add(TYPE);
		components.add(group);
	}

	enum Parser implements IdUtil.Parser<GroupId> {
		INSTANCE;

		@Override
		public GroupId parseInternally(IdIterator parts) {
			return (GroupId) PermissionOwnerId.Parser.INSTANCE.parse(parts);
		}
	}

	@Override
	public Group getPermissionOwner(MetaStorage storage) {
		return storage.getGroup(this);
	}
}
