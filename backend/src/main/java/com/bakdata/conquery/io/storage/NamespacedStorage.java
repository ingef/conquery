package com.bakdata.conquery.io.storage;

import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.caffeine.MetricsStatsCounter;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

/**
 * Overlapping storage structure for {@link WorkerStorage} and {@link NamespaceStorage}.
 * The reason for the overlap ist primarily that all this stored members are necessary in the
 * SerDes communication between the manager and the shards/worker for the resolving of ids included in
 * messages (see also {@link com.bakdata.conquery.io.jackson.serializer.NsIdRef}).
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public abstract class NamespacedStorage extends ConqueryStorage implements NsIdResolver, Injectable {

	@Getter
	@ToString.Include
	private final String pathName;
	@Getter
	private final StoreFactory storageFactory;

	protected SingletonStore<Dataset> dataset;
	protected IdentifiableStore<SecondaryIdDescription> secondaryIds;
	protected IdentifiableStore<Table> tables;
	protected IdentifiableStore<Import> imports;
	protected IdentifiableStore<Concept<?>> concepts;

	protected LoadingCache<Id<?>, Identifiable<?>> cache;
	private Dataset cachedDataset;

	public NamespacedStorage(StoreFactory storageFactory, String pathName) {
		this.pathName = pathName;
		this.storageFactory = storageFactory;
	}

	public void openStores(ObjectMapper objectMapper, MetricRegistry metricRegistry) {
		if (objectMapper != null) {
			injectInto(objectMapper);
		}

		dataset = storageFactory.createDatasetStore(pathName, objectMapper);
		secondaryIds = storageFactory.createSecondaryIdDescriptionStore(pathName, objectMapper);
		tables = storageFactory.createTableStore(pathName, objectMapper);
		imports = storageFactory.createImportStore(pathName, objectMapper);
		concepts = storageFactory.createConceptStore(pathName, objectMapper);

		decorateDatasetStore(dataset);
		decorateSecondaryIdDescriptionStore(secondaryIds);
		decorateTableStore(tables);
		decorateImportStore(imports);
		decorateConceptStore(concepts);


		cache = Caffeine.from(storageFactory.getCacheSpec())
						.recordStats(() -> new MetricsStatsCounter(metricRegistry, pathName + "-cache"))
						.build(new CacheLoader<>() {

                            @Nullable
                            @Override
                            public Identifiable<?> load(Id<?> key) throws Exception {
                                return getFromStorage((Id<?> & NamespacedId) key);
                            }
                        });
	}

	@Override
	public ImmutableList<ManagedStore> getStores() {
		return ImmutableList.of(dataset, secondaryIds, tables, imports, concepts);
	}

	private void decorateDatasetStore(SingletonStore<Dataset> store) {
	}

	private void decorateSecondaryIdDescriptionStore(IdentifiableStore<SecondaryIdDescription> store) {
		// Nothing to decorate
	}

	private void decorateTableStore(IdentifiableStore<Table> store) {

	}

	private void decorateConceptStore(IdentifiableStore<Concept<?>> store) {
		store.onAdd(concept -> {

			if (concept.getDataset() != null && !concept.getDataset().equals(dataset.get().getId())) {
				throw new IllegalStateException("Concept is not for this dataset.");
			}

			concept.setDataset(dataset.get().getId());

		});
	}

	private void decorateImportStore(IdentifiableStore<Import> store) {
		// Intentionally left blank
	}

	// Imports

	public void addImport(Import imp) {
		imports.add(imp);
		cache.invalidate(imp.getId());
	}

	public Import getImport(ImportId id) {
		return get(id);
	}

	private Import getImportFromStorage(ImportId id) {
		return imports.get(id);
	}

	public Stream<Import> getAllImports() {
		return imports.getAll();
	}

	public void updateImport(Import imp) {
		imports.update(imp);
		cache.refresh(imp.getId());
	}

	public void removeImport(ImportId id) {
		imports.remove(id);
		cache.invalidate(id);
	}

	// Datasets

	public Dataset getDataset() {
		// TODO this is not ideal
		Dataset local = cachedDataset;
		if (local == null) {
			local = dataset.get();
			cachedDataset = local;
		}
		return local;
	}


	public void updateDataset(Dataset dataset) {
		this.dataset.update(dataset);
		cachedDataset = dataset;
		cache.refresh(dataset.getId());
	}

	// Tables

	public Table getTable(TableId tableId) {
		return get(tableId);
	}

	private Table getTableFromStorage(TableId tableId) {
		return tables.get(tableId);
	}

	public Stream<Table> getTables() {
		return tables.getAllKeys().map(TableId.class::cast).map(this::get);
	}


	public void addTable(Table table) {
		tables.add(table);
		cache.invalidate(table.getId());
	}

	public void removeTable(TableId table) {
		tables.remove(table);
		cache.invalidate(table);
	}

	// SecondaryId

	public SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId) {
		return get(descriptionId);
	}

	private SecondaryIdDescription getSecondaryIdFromStorage(SecondaryIdDescriptionId descriptionId) {
		return secondaryIds.get(descriptionId);
	}

	public Stream<SecondaryIdDescription> getSecondaryIds() {
		return secondaryIds.getAllKeys().map(SecondaryIdDescriptionId.class::cast).map(this::get);
	}

	public void addSecondaryId(SecondaryIdDescription secondaryIdDescription) {
		secondaryIds.add(secondaryIdDescription);
		cache.invalidate(secondaryIdDescription.getId());
	}

	public void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId) {
		secondaryIds.remove(secondaryIdDescriptionId);
		cache.invalidate(secondaryIdDescriptionId);
	}

	// Concepts

	public Concept<?> getConcept(ConceptId id) {
		return get(id);
	}

	private Concept<?> getConceptFromStorage(ConceptId id) {
		return concepts.get(id);
	}

	public Stream<Concept<?>> getAllConcepts() {
		return concepts.getAllKeys().map(ConceptId.class::cast).map(this::get);
	}

	public boolean hasConcept(ConceptId id) {
		return concepts.get(id) != null;
	}

	@SneakyThrows
	public void updateConcept(Concept<?> concept) {
		log.debug("Updating Concept[{}]", concept.getId());
		concepts.update(concept);
		cache.refresh(concept.getId());
	}

	public void removeConcept(ConceptId id) {
		log.debug("Removing Concept[{}]", id);
		concepts.remove(id);
		cache.invalidate(id);
	}

	// Utility

	public <ID extends Id<?> & NamespacedId, VALUE> VALUE get(ID id) {
		// First check if id belongs to an instance, that is nested in a primary storage instance
		if (id instanceof ColumnId castId) {
			return (VALUE) getTable(castId.getTable()).getColumnByName(castId.getColumn());
		}
		if (id instanceof ConnectorId castId) {
			return (VALUE) getConcept(castId.getConcept()).getConnectorByName(castId.getConnector());
		}
		if (id instanceof ConceptSelectId castId) {
			final ConceptId concept = castId.findConcept();
			return (VALUE) getConcept(concept).getSelectByName(castId.getSelect());
		}
		if (id instanceof ConnectorSelectId castId) {
			final ConceptId concept = castId.findConcept();
			return (VALUE) getConcept(concept).getConnectorByName(castId.getConnector().getConnector()).getSelectByName(castId.getSelect());
		}
		if (id instanceof FilterId castId) {
			final ConnectorId connector = castId.getConnector();
			return (VALUE) getConcept(connector.getConcept()).getConnectorByName(connector.getConnector()).getFilterByName(castId.getFilter());
		}
		if (id instanceof ConceptTreeChildId castId) {
			return (VALUE) getConcept(castId.findConcept()).findById(castId);
		}
		if (id instanceof ValidityDateId castId) {
			return (VALUE) getConcept(castId.getConnector().getConcept()).getConnectorByName(castId.getConnector().getConnector())
					.getValidityDateByName(castId.getValidityDate());
		}

		// get primary storage instance
		return (VALUE) cache.get(id);
	}

	/**
	 * This is just for convenience to have a single function for the cache to load primary storage instances
	 */
	protected <ID extends Id<?> & NamespacedId, VALUE extends Identifiable<?>> VALUE getFromStorage(ID id) {
		if (id instanceof DatasetId castId) {
			final Dataset dataset = getDataset();
			if (dataset.getId().equals(castId)) {
				return (VALUE) dataset;
			}
			return null;
		}
		if (id instanceof ImportId castId) {
			return (VALUE) getImportFromStorage(castId);
		}
		if (id instanceof SecondaryIdDescriptionId castId) {
			return (VALUE) getSecondaryIdFromStorage(castId);
		}
		if (id instanceof TableId castId) {
			return (VALUE) getTableFromStorage(castId);
		}
		if (id instanceof ConceptId castId) {
			return (VALUE) getConceptFromStorage(castId);
		}

		throw new IllegalArgumentException("Id type '" + id.getClass() + "' is not supported");
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NsIdResolver.class, this).
				add(NamespacedStorage.class, this);
	}
}
