package com.bakdata.conquery.models.worker;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public class ShardId extends Id<ShardNodeInformation> {
	private final String host;



	@Override
	public void collectComponents(List<Object> components) {
		components.add(host);
	}

	public static enum Parser implements IdUtil.Parser<ShardId> {
		INSTANCE;

		@Override
		public ShardId parseInternally(IdIterator parts) {
			return new ShardId(parts.next());
		}
	}
}
