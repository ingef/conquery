package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.google.common.collect.PeekingIterator;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class ManagedQueryId extends AId<ManagedQuery> implements NamespacedId {

	private final DatasetId dataset;
	private final UUID query;
	
	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(query);
	}
	
	public static enum Parser implements IId.Parser<ManagedQueryId> {
		INSTANCE;
		
		@Override
		public ManagedQueryId parse(PeekingIterator<String> parts) {
			return new ManagedQueryId(DatasetId.Parser.INSTANCE.parse(parts), UUID.fromString(parts.next()));
		}
	}
}
