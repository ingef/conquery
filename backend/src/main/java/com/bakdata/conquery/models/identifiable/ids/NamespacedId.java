package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

/**
 * Marker interface for {@link Id}s that are bound to a {@link com.bakdata.conquery.models.worker.Namespace}/{@link com.bakdata.conquery.models.datasets.Dataset}.
 */
public abstract class NamespacedId<T> extends Id<T> {

	public static WorkerStorage assertWorkerStorage(NamespacedStorage storage) {
		if (!(storage instanceof WorkerStorage workerStorage)) {
			throw new IllegalArgumentException("Cannot be retrieved from %s".formatted(storage));
		}
		return workerStorage;
	}

	public static NamespaceStorage assertNamespaceStorage(NamespacedStorage storage) {
		if (!(storage instanceof NamespaceStorage namespaceStorage)) {
			throw new IllegalArgumentException("Cannot be retrieved from %s".formatted(storage));
		}
		return namespaceStorage;
	}

	public String toStringWithoutDataset() {
		return StringUtils.removeStart(toString(), getDataset().toString() + IdUtil.JOIN_CHAR);
	}

	@JsonIgnore
	public abstract DatasetId getDataset();

	@JsonIgnore
	public NamespacedStorage getStorage() {
		return getDataset().getStorage();
	}

}