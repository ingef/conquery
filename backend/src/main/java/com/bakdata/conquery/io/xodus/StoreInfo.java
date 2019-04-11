package com.bakdata.conquery.io.xodus;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.xodus.stores.BigStore;
import com.bakdata.conquery.io.xodus.stores.CachedStore;
import com.bakdata.conquery.io.xodus.stores.IStoreInfo;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.MPStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.BlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.models.worker.WorkerInformation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum StoreInfo implements IStoreInfo {
	DATASET			("DATASET", 		Dataset.class,				Boolean.class),
	ID_MAPPING		("ID_MAPPING", 		PersistentIdMap.class,		Boolean.class),
	NAMESPACES		("NAMESPACES", 		Namespaces.class,			Boolean.class),
	SLAVE			("NETWORK_SLAVE", 	SlaveInformation.class,		Boolean.class),
	DICTIONARIES	("DICTIONARIES", 	Dictionary.class,			DictionaryId.class),
	IMPORTS			("IMPORTS", 		Import.class,				ImportId.class),
	CONCEPTS		("CONCEPTS", 		Concept.class,				ConceptId.class),
	BLOCKS			("BLOCKS", 			Block.class,				BlockId.class),
	C_BLOCKS		("C_BLOCKS", 		CBlock.class,				CBlockId.class),
	WORKER			("WORKER",			WorkerInformation.class,	Boolean.class),
	QUERIES			("QUERIES", 		ManagedQuery.class,			ManagedQueryId.class),
	AUTH_PERMISSIONS("AUTH_PERMISSIONS",ConqueryPermission.class,	PermissionId.class),
	AUTH_MANDATOR	("AUTH_MANDATOR", 	Mandator.class,				MandatorId.class),
	AUTH_USER		("AUTH_USER", 		User.class,					UserId.class),
	STRUCTURE		("STRUCTURE", 		StructureNode[].class,		Boolean.class),
	;
	
	private final String xodusName;
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
}
