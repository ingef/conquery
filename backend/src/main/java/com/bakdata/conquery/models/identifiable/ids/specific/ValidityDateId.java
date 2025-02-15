package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ValidityDateId extends Id<ValidityDate> implements NamespacedId {
	private final ConnectorId connector;
	private final String validityDate;

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	@Override
	public NamespacedIdentifiable<?> get(NamespacedStorage storage) {
		return storage.getConcept(getConnector().getConcept())
					  .getConnectorByName(getConnector().getConnector())
					  .getValidityDateByName(getValidityDate());
	}

	@Override
	public void collectComponents(List<Object> components) {
		connector.collectComponents(components);
		components.add(validityDate);
	}

	@Override
	public void collectIds(Collection<? super Id<?>> collect) {
		collect.add(this);
		connector.collectIds(collect);
	}

	@Override
	public NamespacedStorageProvider getNamespacedStorageProvider() {
		return connector.getNamespacedStorageProvider();
	}

	public static enum Parser implements IdUtil.Parser<ValidityDateId> {
		INSTANCE;

		@Override
		public ValidityDateId parseInternally(IdIterator parts) {
			String name = parts.next();
			ConnectorId connector = ConnectorId.Parser.INSTANCE.parse(parts);
			return new ValidityDateId(connector, name);
		}
	}
}
