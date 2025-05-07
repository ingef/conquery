package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @EqualsAndHashCode(callSuper=true)
public class ConnectorSelectId extends SelectId<Select> {

	private final ConnectorId connector;
	
	public ConnectorSelectId(ConnectorId connector, String select) {
		super(select);
		this.connector = connector;
	}

	@Override
	public void collectIds(Collection<Id<?,?>> collect) {
		collect.add(this);
		connector.collectIds(collect);
	}

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	@Override
	public Select get(NamespacedStorageProvider storage) {
		return storage.getStorage(getDataset()).getConcept(findConcept()).getConnectorByName(getConnector().getConnector()).getSelectByName(getSelect());
	}

	@Override
	public ConceptId findConcept() {
		return connector.getConcept();
	}

	@Override
	public void collectComponents(List<Object> components) {
		connector.collectComponents(components);
		super.collectComponents(components);
	}


	public enum Parser implements IdUtil.Parser<ConnectorSelectId> {
		INSTANCE;

		@Override
		public ConnectorSelectId parseInternally(IdIterator parts) {
			String name = parts.next();
			ConnectorId parent = ConnectorId.Parser.INSTANCE.parse(parts);
			return new ConnectorSelectId(parent, name);
		}
	}
}