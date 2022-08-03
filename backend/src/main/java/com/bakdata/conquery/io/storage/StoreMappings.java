package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.storage.xodus.stores.CachedStore;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.storage.xodus.stores.StoreInfo;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Enums and helper methods to create stores of a certain kind.
 * <p>
 * Boolean is used as a placeholder value/class for singleton stores.
 */
@RequiredArgsConstructor
@Getter
@ToString(of = {"name", "keyType", "valueType"})
public enum StoreMappings {
	DATASET(Dataset.class, Boolean.class),
	ID_MAPPING(EntityIdMap.class, Boolean.class),
	NAMESPACES(DatasetRegistry.class, Boolean.class),
	DICTIONARIES(Dictionary.class, DictionaryId.class),
	IMPORTS(Import.class, ImportId.class),
	SECONDARY_IDS(SecondaryIdDescription.class, SecondaryIdDescriptionId.class),
	TABLES(Table.class, TableId.class),
	CONCEPTS(Concept.class, ConceptId.class),
	BUCKETS(Bucket.class, BucketId.class),
	C_BLOCKS(CBlock.class, CBlockId.class),
	WORKER(WorkerInformation.class, Boolean.class),
	EXECUTIONS(ManagedExecution.class, ManagedExecutionId.class),
	AUTH_ROLE(Role.class, RoleId.class),
	AUTH_USER(User.class, UserId.class),
	AUTH_GROUP(Group.class, GroupId.class),
	STRUCTURE(StructureNode[].class, Boolean.class),
	FORM_CONFIG(FormConfig.class, FormConfigId.class),
	WORKER_TO_BUCKETS(WorkerToBucketsMap.class, Boolean.class),
	PRIMARY_DICTIONARY(Dictionary.class, Boolean.class),
	INTERN_TO_EXTERN(InternToExternMapper.class, InternToExternMapperId.class),
	SEARCH_INDEX(SearchIndex.class, SearchIndexId.class);

	private final Class<?> valueType;
	private final Class<?> keyType;

	public <KEY, VALUE, CLASS_K extends Class<KEY>, CLASS_V extends Class<VALUE>> StoreInfo<KEY, VALUE> storeInfo() {
		return new StoreInfo<KEY, VALUE>(getName(), (CLASS_K) getKeyType(), (CLASS_V) getValueType());
	}

	/**
	 * Store for identifiable values, with injectors. Store is also cached.
	 */
	public static <T extends Identifiable<?>> DirectIdentifiableStore<T> identifiable(Store<Id<T>, T> baseStore, CentralRegistry centralRegistry) {
		return new DirectIdentifiableStore<>(centralRegistry, baseStore);
	}

	/**
	 * General Key-Value store with caching.
	 */
	public static <KEY, VALUE> CachedStore<KEY, VALUE> cached(Store<KEY, VALUE> baseStore) {
		return new CachedStore<>(baseStore);
	}

	/**
	 * Identifiable store, that lazy registers items in the central registry.
	 */
	public static <T extends Identifiable<?>> IdentifiableCachedStore<T> identifiableCachedStore(Store<Id<T>, T> baseStore, CentralRegistry centralRegistry) {
		return new IdentifiableCachedStore<T>(centralRegistry, baseStore);
	}


	/**
	 * Store holding a single value.
	 */
	public static <VALUE> SingletonStore<VALUE> singleton(Store<Boolean, VALUE> baseStore) {
		return new SingletonStore<>(baseStore);
	}

	private String getName() {
		return name();
	}
}
