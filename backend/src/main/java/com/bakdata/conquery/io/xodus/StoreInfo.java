package com.bakdata.conquery.io.xodus;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.xodus.stores.BigStore;
import com.bakdata.conquery.io.xodus.stores.CachedStore;
import com.bakdata.conquery.io.xodus.stores.IStoreInfo;
import com.bakdata.conquery.io.xodus.stores.IdentifiableCachedStore;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.xodus.stores.WeakCachedStore;
import com.bakdata.conquery.io.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfigInternal;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.models.worker.WorkerInformation;
import jetbrains.exodus.env.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enums and helper methods to create stores of a certain kind.
 *
 * Boolean is used as a placeholder value/class for singleton stores.
 */
@RequiredArgsConstructor
@Getter
public enum StoreInfo implements IStoreInfo {
	DATASET(Dataset.class, Boolean.class),
	ID_MAPPING(PersistentIdMap.class, Boolean.class),
	NAMESPACES(Namespaces.class, Boolean.class),
	SLAVE(SlaveInformation.class, Boolean.class),
	DICTIONARIES(Dictionary.class, DictionaryId.class),
	IMPORTS(Import.class, ImportId.class),
	CONCEPTS(Concept.class, ConceptId.class),
	BUCKETS(Bucket.class, BucketId.class),
	C_BLOCKS(CBlock.class, CBlockId.class),
	WORKER(WorkerInformation.class, Boolean.class),
	EXECUTIONS(ManagedExecution.class, ManagedExecutionId.class),
	AUTH_ROLE(Role.class, RoleId.class),
	AUTH_USER(User.class, UserId.class),
	AUTH_GROUP(Group.class, GroupId.class),
	STRUCTURE(StructureNode[].class, Boolean.class),
	FORM_CONFIG(FormConfigInternal.class, FormConfigId.class)
	;

	private final Class<?> valueType;
	private final Class<?> keyType;

	/**
	 * Store for identifiable values, with injectors. Store is also cached.
	 */
	public <T extends Identifiable<?>> IdentifiableStore<T> identifiable(Environment environment, Validator validator, CentralRegistry centralRegistry, Injectable... injectables) {

		final CachedStore<IId<T>, T> store = cached(environment, validator);

		for (Injectable injectable : injectables) {
			store.inject(injectable);
		}

		store.inject(centralRegistry);

		return new IdentifiableStore<>(centralRegistry, store);
	}

	/**
	 * Store for identifiable values, without injectors. Store is also cached.
	 */
	public <T extends Identifiable<?>> IdentifiableStore<T> identifiable(Environment environment, Validator validator, CentralRegistry centralRegistry) {
		return identifiable(environment, validator, centralRegistry, new SingletonNamespaceCollection(centralRegistry));
	}

	/**
	 * General Key-Value store with caching.
	 */
	public <KEY, VALUE> CachedStore<KEY, VALUE> cached(Environment environment, Validator validator) {
		return new CachedStore<>(
				new SerializingStore<>(
						new XodusStore(environment, this),
						validator,
						this
				)
		);
	}

	/**
	 * Store holding a single value.
	 */
	public <VALUE> SingletonStore<VALUE> singleton(Environment environment, Validator validator, Injectable... injectables) {
		return new SingletonStore<>(cached(environment, validator), injectables);
	}

	/**
	 * Identifiable store with split Data and Metadata.
	 */
	public <T extends Identifiable<?>> IdentifiableStore<T> big(Environment environment, Validator validator, CentralRegistry centralRegistry) {
		return new IdentifiableStore<>(
				centralRegistry,
				new CachedStore<>(
						new BigStore<>(validator, environment, this)
				)
		);
	}

	/**
	 * Big-Store with weakly held cache.
	 */
	public <T extends Identifiable<?>> IdentifiableCachedStore<T> weakBig(Environment environment, Validator validator, CentralRegistry centralRegistry) {
		return new IdentifiableCachedStore<>(
				centralRegistry,
				new WeakCachedStore<>(
						new BigStore<>(validator, environment, this)
				)
		);
	}

	@Override
	public String getXodusName() {
		return name();
	}
}
