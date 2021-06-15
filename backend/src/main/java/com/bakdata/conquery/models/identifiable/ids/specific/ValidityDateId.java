package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode(callSuper=false)
public class ValidityDateId extends AId<ValidityDate> implements NamespacedId {
	private final ConnectorId connector;
	private final String validityDate;

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}
	
	@Override
	public void collectComponents(List<Object> components) {
		connector.collectComponents(components);
		components.add(validityDate);
	}
	
	public static enum Parser implements IId.Parser<ValidityDateId> {
		INSTANCE;
		
		@Override
		public ValidityDateId parseInternally(IdIterator parts) {
			String name = parts.next();
			ConnectorId connector = ConnectorId.Parser.INSTANCE.parse(parts);
			return new ValidityDateId(connector, name);
		}
	}
}
