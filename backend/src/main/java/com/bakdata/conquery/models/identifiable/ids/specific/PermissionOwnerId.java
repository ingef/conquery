package com.bakdata.conquery.models.identifiable.ids.specific;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor @EqualsAndHashCode(callSuper=false)
public abstract class PermissionOwnerId<T extends PermissionOwner<?>> extends AId<T> {
	

	public enum Parser implements IId.Parser<PermissionOwnerId<?>> {
		INSTANCE;
		
		@Override
		public PermissionOwnerId<?> parseInternally(IdIterator parts) {
			String ownerId = parts.next();
			String type = parts.next();
			return switch (type) {
				case UserId.TYPE -> new UserId(ownerId);
				case RoleId.TYPE -> new RoleId(ownerId);
				case GroupId.TYPE -> new GroupId(ownerId);
				default -> throw new IllegalStateException("Unknown permission owner type: " + type);
			};
		}
	}
	
	public abstract T getPermissionOwner(MetaStorage storage);
}
