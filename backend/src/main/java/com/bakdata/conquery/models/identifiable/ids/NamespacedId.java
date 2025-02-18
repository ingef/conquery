package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.identifiable.IdResolvingException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Marker interface for {@link Id}s that are bound to a {@link com.bakdata.conquery.models.worker.Namespace}/{@link com.bakdata.conquery.models.datasets.Dataset}.
 */
public interface NamespacedId {

	static WorkerStorage assertWorkerStorage(NamespacedStorage storage) {
		if (!(storage instanceof WorkerStorage workerStorage)) {
			throw new IllegalArgumentException("Cannot be retrieved from %s".formatted(storage));
		}
		return workerStorage;
	}

	static NamespaceStorage assertNamespaceStorage(NamespacedStorage storage) {
		if (!(storage instanceof NamespaceStorage namespaceStorage)) {
			throw new IllegalArgumentException("Cannot be retrieved from %s".formatted(storage));
		}
		return namespaceStorage;
	}

	default String toStringWithoutDataset() {
		return StringUtils.removeStart(toString(), getDataset().toString() + IdUtil.JOIN_CHAR);
	}

	@JsonIgnore
	DatasetId getDataset();

	/**
	 * Almost identical to {@link NamespacedId#get(NamespacedStorage)}, but throws an {@link IdResolvingException} if no object could be resolved.
	 *
	 * @return the object or throws an {@link IdResolvingException} if the Object could not be resolved.
	 */
	@NotNull
	default <T extends NamespacedIdentifiable<?>> T resolve(NamespacedStorage storage) {
		try {
			NamespacedIdentifiable<?> o = get(storage);
			if (o == null) {
				throw newIdResolveException();
			}
			return (T) o;
		}
		catch (IdResolvingException e) {
			throw e;
		}
		catch (Exception e) {
			throw newIdResolveException(e);
		}
	}

	/**
	 * Return the object identified by the given id from the given storage.
	 *
	 * @return the object or null if no object could be resolved. If the id type is not supported
	 * throws a IllegalArgumentException
	 */
	NamespacedIdentifiable<?> get(NamespacedStorage storage);

	IdResolvingException newIdResolveException();

	IdResolvingException newIdResolveException(Exception e);
}