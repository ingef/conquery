package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.google.common.collect.PeekingIterator;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class PermissionId extends AId<ConqueryPermission> {
	
	private final UUID permission;
	
	public void collectComponents(List<Object> components) {
		components.add(permission);
	}
	

	enum Parser implements IId.Parser<PermissionId> {
		INSTANCE;
		
		@Override
		public PermissionId parse(PeekingIterator<String> parts) {
			return new PermissionId(UUID.fromString(parts.next()));
		}
	}
}
