package com.bakdata.conquery.models.identifiable.ids.specific;

import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract sealed class PermissionOwnerId<T extends PermissionOwner<?>> extends MetaId<T>
		permits UserId, RoleId, GroupId {


	public enum Parser implements IdUtil.Parser<PermissionOwnerId<?>> {
		INSTANCE;

		@Override
		public PermissionOwnerId<?> parseInternally(IdIterator parts) {
			final String ownerId = parts.next();
			final String type = parts.next();

			return switch (type) {
				case UserId.TYPE -> new UserId(ownerId);
				case RoleId.TYPE -> new RoleId(ownerId);
				case GroupId.TYPE -> new GroupId(ownerId);
				default -> throw new IllegalStateException("Unknown permission owner type: " + type);
			};
		}
	}

}
