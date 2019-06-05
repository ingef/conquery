package com.bakdata.conquery.io.xodus;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.xodus.stores.BigStore;
import com.bakdata.conquery.io.xodus.stores.CachedStore;
import com.bakdata.conquery.io.xodus.stores.IStoreInfo;
import com.bakdata.conquery.io.xodus.stores.IdentifiableCachedStore;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.MPStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.xodus.stores.WeakCachedStore;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.models.worker.WorkerInformation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum StoreInfo implements IStoreInfo {
	DATASET			(Dataset.class,				Boolean.class),
	ID_MAPPING		(PersistentIdMap.class,		Boolean.class),
	NAMESPACES		(Namespaces.class,			Boolean.class),
	SLAVE			(SlaveInformation.class,		Boolean.class),
	DICTIONARIES	(Dictionary.class,			DictionaryId.class),
	IMPORTS			(Import.class,				ImportId.class),
	CONCEPTS		(Concept.class,				ConceptId.class),
	BUCKETS			(Bucket.class,				BucketId.class),
	C_BLOCKS		(CBlock.class,				CBlockId.class),
	WORKER			(WorkerInformation.class,	Boolean.class),
	EXECUTIONS		(ManagedExecution.class,		ManagedExecutionId.class),
	AUTH_PERMISSIONS(ConqueryPermission.class,	PermissionId.class),
	AUTH_MANDATOR	(Mandator.class,				MandatorId.class),
	AUTH_USER		(User.class,					UserId.class),
	STRUCTURE		(StructureNode[].class,		Boolean.class),
	;
	
	private final Class<?> valueType;
	private final Class<?> keyType;
	
	public <T extends Identifiable<?>> IdentifiableStore<T> identifiable(ConqueryStorage storage, Injectable... injectables) {
		return new IdentifiableStore<>(
			storage.getCentralRegistry(),
			cached(storage),
			injectables
		);
	}
	
	public <T extends Identifiable<?>> IdentifiableStore<T> identifiable(ConqueryStorage storage) {
		return new IdentifiableStore<>(
			storage.getCentralRegistry(),
			cached(storage)
		);
	}

	public <KEY, VALUE> CachedStore<KEY, VALUE> cached(ConqueryStorage storage) {
		return new CachedStore<>(
			new MPStore<>(storage.getValidator(), storage.getEnvironment(), this)
		);
	}
	
	public <VALUE> SingletonStore<VALUE> singleton(ConqueryStorage storage, Injectable... injectables) {
		return new SingletonStore<>(cached(storage), injectables);
	}
	
	public <T extends Identifiable<?>> IdentifiableStore<T> big(NamespacedStorage storage) {
		return new IdentifiableStore<>(
			storage.getCentralRegistry(),
			new CachedStore<>(
				new BigStore<>(storage.getValidator(), storage.getEnvironment(), this)
			)
		);
	}

	@Override
	public String getXodusName() {
		return name();
	}
	
	public <T extends Identifiable<?>> IdentifiableCachedStore<T> weakBig(NamespacedStorage storage) {
		return new IdentifiableCachedStore<>(
			storage.getCentralRegistry(),
			new WeakCachedStore<>(
				new BigStore<>(storage.getValidator(), storage.getEnvironment(), this)
			)
		);
	}
}
