package com.bakdata.conquery.io.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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

	@Getter
	private final Validator validator;

	protected SingletonStore<Dataset> dataset;
	protected IdentifiableStore<SecondaryIdDescription> secondaryIds;
	protected IdentifiableStore<Table> tables;
	protected IdentifiableStore<Import> imports;
	protected IdentifiableStore<Concept<?>> concepts;

	private LoadingCache<Id<?>, Identifiable<?>> cache;

	public NamespacedStorage(StoreFactory storageFactory, String pathName, Validator validator) {
		this.pathName = pathName;
		this.storageFactory = storageFactory;
		this.validator = validator;
	}

	public void openStores(ObjectMapper objectMapper) {
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
						.build(new CacheLoader<Id<?>, Identifiable<?>>() {

							@Nullable
							@Override
							public Identifiable<?> load(Id<?> key) throws Exception {
								return getFromStorage((Id & NamespacedId) key);
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

			if (concept.getDataset() != null && !concept.getDataset().equals(dataset.get())) {
				throw new IllegalStateException("Concept is not for this dataset.");
			}

			concept.setDataset(dataset.get());

			concept.initElements();

			if (log.isTraceEnabled()) {
				// Validating concepts is quite slow, so we only validate when requested.
				final Set<ConstraintViolation<Concept<?>>> violations = validator.validate(concept);

				ValidatorHelper.failOnError(log, violations);
			}

		});
	}

	private void decorateImportStore(IdentifiableStore<Import> store) {
		// Intentionally left blank
	}


	public void addImport(Import imp) {
		imports.add(imp);
	}

	public Import getImport(ImportId id) {
		return imports.get(id);
	}

	public Collection<Import> getAllImports() {
		return imports.getAll();
	}

	public void updateImport(Import imp) {
		imports.update(imp);
	}

	public void removeImport(ImportId id) {
		imports.remove(id);
	}

	public Dataset getDataset() {
		return dataset.get();
	}

	public void updateDataset(Dataset dataset) {
		this.dataset.update(dataset);
	}

	public List<Table> getTables() {
		return new ArrayList<>(tables.getAll());
	}

	public Table getTable(TableId tableId) {
		return tables.get(tableId);
	}

	public void addTable(Table table) {
		tables.add(table);
	}

	public void removeTable(TableId table) {
		tables.remove(table);
	}

	public List<SecondaryIdDescription> getSecondaryIds() {
		return new ArrayList<>(secondaryIds.getAll());
	}

	public SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId) {
		return secondaryIds.get(descriptionId);
	}

	public void addSecondaryId(SecondaryIdDescription secondaryIdDescription) {
		secondaryIds.add(secondaryIdDescription);
	}

	public void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId) {
		secondaryIds.remove(secondaryIdDescriptionId);
	}

	public Concept<?> getConcept(ConceptId id) {
		return concepts.get(id);
	}

	public boolean hasConcept(ConceptId id) {
		return concepts.get(id) != null;
	}

	@SneakyThrows
	public void updateConcept(Concept<?> concept) {
		log.debug("Updating Concept[{}]", concept.getId());
		concepts.update(concept);
	}

	public void removeConcept(ConceptId id) {
		log.debug("Removing Concept[{}]", id);
		concepts.remove(id);
	}

	public Collection<Concept<?>> getAllConcepts() {
		return concepts.getAll();
	}


	public <ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<?>> VALUE get(ID id) {
		return (VALUE) cache.get(id);
	}

	protected <ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<?>> VALUE getFromStorage(ID id) {
		if (id instanceof DatasetId castId) {
			final Dataset dataset = getDataset();
			if (dataset.getId().equals(castId)) {
				return (VALUE) dataset;
			}
			return null;
		}
		if (id instanceof ImportId castId) {
			return (VALUE) getImport(castId);
		}
		if (id instanceof SecondaryIdDescriptionId castId) {
			return (VALUE) getSecondaryId(castId);
		}
		if (id instanceof TableId castId) {
			return (VALUE) getTable(castId);
		}
		if (id instanceof ConceptId castId) {
			return (VALUE) getConcept(castId);
		}
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
			return (VALUE) ((TreeConcept) getConcept(castId.findConcept())).findById(castId);
		}
		if (id instanceof ValidityDateId castId) {
			return (VALUE) getConcept(castId.getConnector().getConcept()).getConnectorByName(castId.getConnector().getConnector())
																		 .getValidityDateByName(castId.getValidityDate());
		}

		throw new IllegalArgumentException("Id type '" + id.getClass() + "' is not supported");
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NsIdResolver.class, this);
	}
}
