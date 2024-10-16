package com.bakdata.conquery.models.identifiable.ids.specific;

import static com.bakdata.conquery.models.identifiable.ids.NamespacedId.assertWorkerStorage;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.events.Bucket;
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
public class BucketId extends Id<Bucket> implements NamespacedId {

	private final ImportId imp;
	private final int bucket;

	@Override
	public DatasetId getDataset() {
		return imp.getDataset();
	}

	@Override
	public NamespacedIdentifiable<?> get(NamespacedStorage storage) {
		return assertWorkerStorage(storage).getBucket(this);
	}

	@Override
	public void collectComponents(List<Object> components) {
		imp.collectComponents(components);
		components.add(bucket);
	}

	@Override
	public void collectIds(Collection<? super Id<?>> collect) {
		collect.add(this);
		imp.collectIds(collect);
	}

	@Override
	public NamespacedStorageProvider getNamespacedStorageProvider() {
		return imp.getNamespacedStorageProvider();
	}

	public static enum Parser implements IdUtil.Parser<BucketId> {
		INSTANCE;

		@Override
		public BucketId parseInternally(IdIterator parts) {
			int bucket = Integer.parseInt(parts.next());
			ImportId parent = ImportId.Parser.INSTANCE.parse(parts);
			return new BucketId(parent, bucket);
		}
	}
}