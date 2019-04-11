package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.google.common.collect.PeekingIterator;

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

	public enum Parser implements IId.Parser<ConnectorSelectId> {
		INSTANCE;
		
		@Override
		public ConnectorSelectId parse(PeekingIterator<String> parts) {
			ConnectorId parent = ConnectorId.Parser.INSTANCE.parse(parts);
			return new ConnectorSelectId(parent, parts.next());
		}
	}
}