package com.bakdata.conquery.models.identifiable.ids;

import java.lang.ref.WeakReference;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Marker interface for {@link Id}s that are bound to a {@link com.bakdata.conquery.models.worker.Namespace}/{@link com.bakdata.conquery.models.datasets.Dataset}.
 */
public abstract non-sealed class NamespacedId<TYPE> implements Id<TYPE, NamespacedStorageProvider> {

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

	/**
	 * Holds the cached escaped value.
	 *
	 * @implNote needs to be initialized. Otherwise, SerializationTests fail, because assertj checks ignored types.
	 */
	@JsonIgnore
	private WeakReference<String> escapedId = new WeakReference<>(null);

	@Setter
	@Getter
	@JacksonInject(useInput = OptBoolean.FALSE)
	@JsonIgnore
	@NonNull
	private NamespacedStorageProvider namespacedStorageProvider;

	@Override
	@JsonIgnore
	public NamespacedStorageProvider getStorage() {
		return namespacedStorageProvider.getStorage(getDataset());
	}

	@Override
	public void setStorage(NamespacedStorageProvider namespacedStorage) {
		namespacedStorageProvider = namespacedStorage;
	}

	@Override
	@JsonValue
	public final String toString() {
		final String escaped = escapedId.get();
		if (escaped != null) {
			return escaped;
		}

		String escapedIdString = escapedIdString();
		escapedId = new WeakReference<>(escapedIdString);
		return escapedIdString;
	}


	public String toStringWithoutDataset() {
		return StringUtils.removeStart(toString(), getDataset().toString() + IdUtil.JOIN_CHAR);
	}

	@JsonIgnore
	public abstract DatasetId getDataset();


}