package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.query.select.Select;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class SelectId extends AId<Select> implements NamespacedId {

	private final ConnectorId connector;
	private final String select;

	@Override
	public void collectComponents(List<Object> components) {
		connector.collectComponents(components);
		components.add(select);
	}

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	public static enum Parser implements IId.Parser<SelectId> {
		INSTANCE;
		
		@Override
		public SelectId parse(Iterator<String> parts) {
			ConnectorId parent = ConnectorId.Parser.INSTANCE.parse(parts);
			return new SelectId(parent, parts.next());
		}
	}
}