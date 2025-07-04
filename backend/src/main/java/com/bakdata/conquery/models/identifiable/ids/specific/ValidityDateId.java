package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ValidityDateId extends NamespacedId<ValidityDate> {
	private final ConnectorId connector;
	private final String validityDate;

	@Override
	public ValidityDate get() {
		return getDomain().getStorage(getDataset())
						  .getConcept(getConnector().getConcept())
						  .getConnectorByName(getConnector().getConnector())
						  .getValidityDateByName(getValidityDate());
	}

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	@Override
	public void collectComponents(List<Object> components) {
		connector.collectComponents(components);
		components.add(validityDate);
	}

	@Override
	public void collectIds(Collection<Id<?, ?>> collect) {
		collect.add(this);
		connector.collectIds(collect);
	}


	public enum Parser implements IdUtil.Parser<ValidityDateId> {
		INSTANCE;

		@Override
		public ValidityDateId parseInternally(IdIterator parts) {
			String name = parts.next();
			ConnectorId connector = ConnectorId.Parser.INSTANCE.parse(parts);
			return new ValidityDateId(connector, name);
		}
	}
}
