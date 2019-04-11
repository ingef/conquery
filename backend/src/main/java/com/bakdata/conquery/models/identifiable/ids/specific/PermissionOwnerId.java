package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.google.common.collect.PeekingIterator;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor @EqualsAndHashCode(callSuper=false)
public abstract class PermissionOwnerId<T extends PermissionOwner<?>> extends AId<T> {
	@Override
	public void collectComponents(List<Object> components) {
	}
	

	public enum Parser implements IId.Parser<PermissionOwnerId<?>> {
		INSTANCE;
		
		@Override
		public PermissionOwnerId<?> parse(PeekingIterator<String> parts) {
			String type = parts.next();
			String ownerId = parts.next();
			switch(type) {
				case UserId.TYPE:
					return new UserId(ownerId);
				case MandatorId.TYPE:
					return new MandatorId(ownerId);
				case NullSubjectId.TYPE:
					return new NullSubjectId();
				default:
					throw new IllegalStateException("Unknown permission owner type: " + type);
			}
		}
	}
	
	public abstract PermissionOwner<?> getOwner(MasterMetaStorage storage);
}
