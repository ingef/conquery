package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @EqualsAndHashCode(callSuper=true)
public class ConnectorSelectId extends SelectId implements NamespacedId {

	private final ConnectorId connector;
	
	public ConnectorSelectId(ConnectorId connector, String select) {
		super(select);
		this.connector = connector;
	}

	@Override
	public void collectIds(Collection<? super Id<?>> collect) {
		collect.add(this);
		connector.collectIds(collect);
	}

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	@Override
	public NamespacedIdentifiable<?> get(NamespacedStorage storage) {
		return storage.getConcept(findConcept()).getConnectorByName(getConnector().getConnector()).getSelectByName(getSelect());
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

	@Override
	public NamespacedStorageProvider getNamespacedStorageProvider() {
		return connector.getNamespacedStorageProvider();
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