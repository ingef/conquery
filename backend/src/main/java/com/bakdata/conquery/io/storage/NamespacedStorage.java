package com.bakdata.conquery.io.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
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
public abstract class NamespacedStorage implements ConqueryStorage {

	@Getter
	protected final CentralRegistry centralRegistry = new CentralRegistry();
	@Getter
	private final Validator validator;
	@Getter @ToString.Include
	private final String pathName;

	protected SingletonStore<Dataset> dataset;
	protected IdentifiableStore<SecondaryIdDescription> secondaryIds;
	protected IdentifiableStore<Table> tables;
	protected IdentifiableStore<Dictionary> dictionaries;
	protected IdentifiableStore<Import> imports;
	protected IdentifiableStore<Concept<?>> concepts;

	public NamespacedStorage(Validator validator, StoreFactory storageFactory, String pathName) {
		this.validator = validator;
		this.pathName = pathName;

		dataset = storageFactory.createDatasetStore(pathName);
		secondaryIds = storageFactory.createSecondaryIdDescriptionStore(centralRegistry, pathName);
		tables = storageFactory.createTableStore(centralRegistry, pathName);
		dictionaries = storageFactory.createDictionaryStore(centralRegistry, pathName);
		imports = storageFactory.createImportStore(centralRegistry, pathName);
		concepts = storageFactory.createConceptStore(centralRegistry, pathName);

		decorateDatasetStore(dataset);
		decorateSecondaryIdDescriptionStore(secondaryIds);
		decorateDictionaryStore(dictionaries);
		decorateTableStore(tables);
		decorateImportStore(imports);
		decorateConceptStore(concepts);

	}

	@Override
	public void loadData() {
		dataset.loadData();
		secondaryIds.loadData();
		tables.loadData();
		dictionaries.loadData();
		imports.loadData();
		concepts.loadData();
		log.info("Done reading {} / {}", dataset.get(), getClass().getName());
	}

	@Override
	public void clear() {
		centralRegistry.clear();

		dataset.clear();
		secondaryIds.clear();
		tables.clear();
		dictionaries.clear();
		imports.clear();
		concepts.clear();
	}

	@Override
	public void removeStorage() {
		dataset.removeStore();
		secondaryIds.removeStore();
		tables.removeStore();
		dictionaries.removeStore();
		imports.removeStore();
		concepts.removeStore();

	}

	protected abstract boolean isRegisterImports();

	private void decorateDatasetStore(SingletonStore<Dataset> store) {
		store.onAdd(centralRegistry::register)
			 .onRemove(centralRegistry::remove);
	}

	private void decorateSecondaryIdDescriptionStore(IdentifiableStore<SecondaryIdDescription> store) {
		// Nothing to decorate
	}

	private void decorateDictionaryStore(IdentifiableStore<Dictionary> store) {
		// Nothing to decorate
	}

	private void decorateTableStore(IdentifiableStore<Table> store) {
		store
				.onAdd(table -> {
					for (Column c : table.getColumns()) {
						getCentralRegistry().register(c);
					}
				})
				.onRemove(table -> {
					for (Column c : table.getColumns()) {
						getCentralRegistry().remove(c);
					}
				});
	}

	private void decorateConceptStore(IdentifiableStore<Concept<?>> store) {
		store
				.onAdd(concept -> {

					if (concept.getDataset() != null && !concept.getDataset().equals(dataset.get())) {
						throw new IllegalStateException("Concept is not for this dataset.");
					}

					concept.setDataset(dataset.get());

					concept.initElements(getValidator());

					concept.getSelects().forEach(centralRegistry::register);
					for (Connector connector : concept.getConnectors()) {
						centralRegistry.register(connector);
						connector.collectAllFilters().forEach(centralRegistry::register);
						connector.getSelects().forEach(centralRegistry::register);
						connector.getValidityDates().forEach(centralRegistry::register);
					}
					//add imports of table
					if (isRegisterImports()) {
						for (Import imp : getAllImports()) {
							for (Connector con : concept.getConnectors()) {
								if (con.getTable().equals(imp.getTable())) {
									con.addImport(imp);
								}
							}
						}
					}

					if (concept instanceof TreeConcept) {
						((TreeConcept) concept).getAllChildren().values().forEach(centralRegistry::register);
					}
				})
				.onRemove(concept -> {
					concept.getSelects().forEach(centralRegistry::remove);
					//see #146  remove from Dataset.concepts
					for (Connector connector : concept.getConnectors()) {
						connector.getSelects().forEach(centralRegistry::remove);
						connector.collectAllFilters().forEach(centralRegistry::remove);
						connector.getValidityDates().forEach(centralRegistry::remove);
						centralRegistry.remove(connector);
					}

					if (concept instanceof TreeConcept) {
						((TreeConcept) concept).getAllChildren().values().forEach(centralRegistry::remove);
					}
				});
	}

	private void decorateImportStore(IdentifiableStore<Import> store) {
		store.onAdd(imp -> {
			if (isRegisterImports()) {
				for (Concept<?> c : getAllConcepts()) {
					for (Connector con : c.getConnectors()) {
						if (con.getTable().equals(imp.getTable())) {
							con.addImport(imp);
						}
					}
				}
			}
		});
	}

	public Dictionary getDictionary(DictionaryId id) {
		return dictionaries.get(id);
	}

	public void updateDictionary(Dictionary dict) {
		dictionaries.update(dict);
	}

	public void removeDictionary(DictionaryId id) {
		dictionaries.remove(id);
	}

	public EncodedDictionary getPrimaryDictionary() {
		return new EncodedDictionary(dictionaries.get(ConqueryConstants.getPrimaryDictionary(getDataset())), StringTypeEncoded.Encoding.UTF8);
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

	public Collection<? extends Concept<?>> getAllConcepts() {
		return concepts.getAll();
	}

	public void close() throws IOException {
		dataset.close();
		secondaryIds.close();
		tables.close();
		dictionaries.close();
		imports.close();
		concepts.close();
	}
}
