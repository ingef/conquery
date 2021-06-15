package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class FilterId extends AId<Filter<?>> implements NamespacedId {

	private final ConnectorId connector;
	private final String filter;
	
	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		connector.collectComponents(components);
		components.add(filter);
	}
	
	public static enum Parser implements IId.Parser<FilterId> {
		INSTANCE;
		
		@Override
		public FilterId parseInternally(IdIterator parts) {
			String filter = parts.next();
			ConnectorId parent = ConnectorId.Parser.INSTANCE.parse(parts);
			return new FilterId(parent, filter);
		}
	}
}