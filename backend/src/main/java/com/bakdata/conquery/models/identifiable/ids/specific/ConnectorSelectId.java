package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

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
	public void collectComponents(List<Object> components) {
		connector.collectComponents(components);
		super.collectComponents(components);
	}

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	public enum Parser implements AId.Parser<ConnectorSelectId> {
		INSTANCE;

		@Override
		public ConnectorSelectId parseInternally(IdIterator parts) {
			String name = parts.next();
			ConnectorId parent = ConnectorId.Parser.INSTANCE.parse(parts);
			return new ConnectorSelectId(parent, name);
		}
	}

	@Override
	public ConceptId findConcept() {
		return connector.getConcept();
	}
}