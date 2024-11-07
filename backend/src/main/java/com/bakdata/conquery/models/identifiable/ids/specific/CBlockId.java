package com.bakdata.conquery.models.identifiable.ids.specific;

import static com.bakdata.conquery.models.identifiable.ids.NamespacedId.assertWorkerStorage;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.events.CBlock;
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
public class CBlockId extends Id<CBlock> implements NamespacedId {

	private final BucketId bucket;
	private final ConnectorId connector;

	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	@Override
	public NamespacedIdentifiable<?> get(NamespacedStorage storage) {
		return assertWorkerStorage(storage).getCBlock(this);
	}

	@Override
	public void collectComponents(List<Object> components) {
		bucket.collectComponents(components);
		connector.collectComponents(components);
	}

	@Override
	public void collectIds(Collection<? super Id<?>> collect) {
		collect.add(this);
		bucket.collectIds(collect);
		connector.collectIds(collect);
	}

	@Override
	public NamespacedStorageProvider getNamespacedStorageProvider() {
		return bucket.getNamespacedStorageProvider();
	}

	public static enum Parser implements IdUtil.Parser<CBlockId> {
		INSTANCE;

		@Override
		public CBlockId parseInternally(IdIterator parts) {
			ConnectorId connector = ConnectorId.Parser.INSTANCE.parse(parts.splitOff(3));
			BucketId block = BucketId.Parser.INSTANCE.parse(parts);
			return new CBlockId(block, connector);
		}
	}
}