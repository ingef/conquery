package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.events.Bucket;
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
public class BucketId extends NamespacedId<Bucket> {

	private final ImportId imp;
	private final int bucket;

	@Override
	public DatasetId getDataset() {
		return imp.getDataset();
	}

	@Override
	public Bucket get() {
		return assertWorkerStorage(getDomain().getStorage(getDataset()))
				.getBucket(this);
	}

	@Override
	public void collectComponents(List<Object> components) {
		imp.collectComponents(components);
		components.add(bucket);
	}

	@Override
	public void collectIds(Collection<Id<?, ?>> collect) {
		collect.add(this);
		imp.collectIds(collect);
	}


	public enum Parser implements IdUtil.Parser<BucketId> {
		INSTANCE;

		@Override
		public BucketId parseInternally(IdIterator parts) {
			int bucket = Integer.parseInt(parts.next());
			ImportId parent = ImportId.Parser.INSTANCE.parse(parts);
			return new BucketId(parent, bucket);
		}
	}
}