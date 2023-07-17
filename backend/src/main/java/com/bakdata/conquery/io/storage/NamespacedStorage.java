package com.bakdata.conquery.io.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import com.bakdata.conquery.io.storage.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Overlapping storage structure for {@link WorkerStorage} and {@link NamespaceStorage}.
 * The reason for the overlap ist primarily that all this stored members are necessary in the
 * SerDes communication between the manager and the shards/worker for the resolving of ids included in
 * messages (see also {@link com.bakdata.conquery.io.jackson.serializer.NsIdRef}).
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public abstract class NamespacedStorage extends ConqueryStorage {

	@Getter
	protected final CentralRegistry centralRegistry = new CentralRegistry();
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

	public NamespacedStorage(StoreFactory storageFactory, String pathName, Validator validator) {
		this.pathName = pathName;
		this.storageFactory = storageFactory;
		this.validator = validator;
	}

	public void openStores(ObjectMapper objectMapper) {


		dataset = storageFactory.createDatasetStore(pathName, objectMapper);
		secondaryIds = storageFactory.createSecondaryIdDescriptionStore(centralRegistry, pathName, objectMapper);
		tables = storageFactory.createTableStore(centralRegistry, pathName, objectMapper);
		imports = storageFactory.createImportStore(centralRegistry, pathName, objectMapper);
		concepts = storageFactory.createConceptStore(centralRegistry, pathName, objectMapper);

		decorateDatasetStore(dataset);
		decorateSecondaryIdDescriptionStore(secondaryIds);
		decorateTableStore(tables);
		decorateImportStore(imports);
		decorateConceptStore(concepts);
	}

	@Override
	public ImmutableList<KeyIncludingStore<?, ?>> getStores() {
		return ImmutableList.of(dataset, secondaryIds, tables, imports, concepts);
	}

	@Override
	public void clear() {
		super.clear();
		centralRegistry.clear();
	}

	private void decorateDatasetStore(SingletonStore<Dataset> store) {
		store.onAdd(centralRegistry::register).onRemove(centralRegistry::remove);
	}

	private void decorateSecondaryIdDescriptionStore(IdentifiableStore<SecondaryIdDescription> store) {
		// Nothing to decorate
	}

	private void decorateTableStore(IdentifiableStore<Table> store) {
		store.onAdd(table -> {
			for (Column c : table.getColumns()) {
				getCentralRegistry().register(c);
			}
		}).onRemove(table -> {
			for (Column c : table.getColumns()) {
				getCentralRegistry().remove(c);
			}
		});
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

			concept.getSelects().forEach(centralRegistry::register);
			for (Connector connector : concept.getConnectors()) {
				centralRegistry.register(connector);
				connector.collectAllFilters().forEach(centralRegistry::register);
				connector.getSelects().forEach(centralRegistry::register);
				connector.getValidityDates().forEach(centralRegistry::register);
			}


			if (concept instanceof TreeConcept) {
				((TreeConcept) concept).getAllChildren().forEach(centralRegistry::register);
			}
		}).onRemove(concept -> {
			concept.getSelects().forEach(centralRegistry::remove);
			//see #146  remove from Dataset.concepts
			for (Connector connector : concept.getConnectors()) {
				connector.getSelects().forEach(centralRegistry::remove);
				connector.collectAllFilters().forEach(centralRegistry::remove);
				connector.getValidityDates().forEach(centralRegistry::remove);
				centralRegistry.remove(connector);
			}

			if (concept instanceof TreeConcept) {
				((TreeConcept) concept).getAllChildren().forEach(centralRegistry::remove);
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
		concepts.update(concept);
	}

	public void removeConcept(ConceptId id) {
		concepts.remove(id);
	}

	public Collection<Concept<?>> getAllConcepts() {
		return concepts.getAll();
	}

}
