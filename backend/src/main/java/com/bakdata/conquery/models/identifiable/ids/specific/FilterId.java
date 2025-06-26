package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
public class FilterId extends NamespacedId<Filter<?>>  {

	private final ConnectorId connector;
	private final String filter;

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	@Override
	public Filter<?> get() {
		return getDomain().getStorage(getDataset())
						  .getConcept(connector.getConcept()).getConnectorByName(connector.getConnector())
						  .getFilterByName(getFilter());
	}

	@Override
	public void collectComponents(List<Object> components) {
		connector.collectComponents(components);
		components.add(filter);
	}

	@Override
	public void collectIds(Collection<Id<?,?>> collect) {
		collect.add(this);
		connector.collectIds(collect);
	}


	public enum Parser implements IdUtil.Parser<FilterId> {
		INSTANCE;

		@Override
		public FilterId parseInternally(IdIterator parts) {
			String filter = parts.next();
			ConnectorId parent = ConnectorId.Parser.INSTANCE.parse(parts);
			return new FilterId(parent, filter);
		}
	}
}