package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class ConnectorId extends AId<Connector> implements NamespacedId {

	private final ConceptId concept;
	private final String connector;
	
	@Override
	public DatasetId getDataset() {
		return concept.getDataset();
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		concept.collectComponents(components);
		components.add(connector);
	}
	
	public static enum Parser implements IId.Parser<ConnectorId> {
		INSTANCE;
		
		@Override
		public ConnectorId parseInternally(IdIterator parts) {
			String connector = parts.next();
			ConceptId parent = ConceptId.Parser.INSTANCE.parse(parts);
			return new ConnectorId(parent, connector);
		}
	}
}
