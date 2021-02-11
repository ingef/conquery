package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.io.xodus.StoreInfo;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.config.StorageFactory;
import com.bakdata.conquery.models.datasets.*;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public abstract class InternalNamespacedStorage implements NamespacedStorage {

    @Getter
    protected final CentralRegistry centralRegistry = new CentralRegistry();
    @Getter
    private final Validator validator;

    protected SingletonStore<Dataset> dataset;
    protected IdentifiableStore<SecondaryIdDescription> secondaryIds;
    protected IdentifiableStore<Table> tables;
    protected IdentifiableStore<Dictionary> dictionaries;
    protected IdentifiableStore<Import> imports;
    protected IdentifiableStore<Concept<?>> concepts;

    public InternalNamespacedStorage(Validator validator, StorageFactory storageFactory, List<String> pathName) {
        this.validator = validator;

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
    }

    @Override
    public void clear() {
        dataset.clear();
        secondaryIds.clear();
        tables.clear();
        dictionaries.clear();
        imports.clear();
        concepts.clear();
    }

    @Override
    public void remove() {
        dataset.remove();
        secondaryIds.remove();
        tables.remove();
        dictionaries.remove();
        imports.remove();
        concepts.remove();

    }

    void decorateDatasetStore(SingletonStore<Dataset> store) {
        store
                .onAdd(getCentralRegistry()::register)
                .onRemove(getCentralRegistry()::remove);
    }

    void decorateSecondaryIdDescriptionStore(IdentifiableStore<SecondaryIdDescription> store) {
        // Nothing to decorate
    }

    void decorateDictionaryStore(IdentifiableStore<Dictionary> store) {
        // Nothing to decorate
    }

    void decorateTableStore(IdentifiableStore<Table> store) {
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

    void decorateConceptStore(IdentifiableStore<Concept<?>> store) {
        store
                .onAdd(concept -> {
                    Dataset ds = centralRegistry.resolve(
                            concept.getDataset() == null
                                    ? concept.getId().getDataset()
                                    : concept.getDataset()
                    );
                    concept.setDataset(ds.getId());

                    concept.initElements(getValidator());

                    concept.getSelects().forEach(centralRegistry::register);
                    for (Connector c : concept.getConnectors()) {
                        centralRegistry.register(c);
                        c.collectAllFilters().forEach(centralRegistry::register);
                        c.getSelects().forEach(centralRegistry::register);
                    }
                    //add imports of table
                    if (isRegisterImports()) {
                        for (Import imp : getAllImports()) {
                            for (Connector con : concept.getConnectors()) {
                                if (con.getTable().getId().equals(imp.getTable())) {
                                    con.addImport(imp);
                                }
                            }
                        }
                    }
                })
                .onRemove(concept -> {
                    concept.getSelects().forEach(centralRegistry::remove);
                    //see #146  remove from Dataset.concepts
                    for (Connector c : concept.getConnectors()) {
                        c.getSelects().forEach(centralRegistry::remove);
                        c.collectAllFilters().stream().map(Filter::getId).forEach(centralRegistry::remove);
                        centralRegistry.remove(c.getId());
                    }
                });
    }

    void decorateImportStore(IdentifiableStore<Import> store) {
        store
                .onAdd(imp -> {
                    imp.loadExternalInfos(this);

                    if (isRegisterImports()) {
                        for (Concept<?> c : getAllConcepts()) {
                            for (Connector con : c.getConnectors()) {
                                if (con.getTable().getId().equals(imp.getTable())) {
                                    con.addImport(imp);
                                }
                            }
                        }
                    }

                    getCentralRegistry().register(imp);

                })
                .onRemove(imp -> {
                    getCentralRegistry().remove(imp);

                });
    }

    void decorateWorkerStore(SingletonStore<WorkerInformation> store) {
        // Nothing to decorate
    }

    void decorateBucketStore(IdentifiableStore<Bucket> store) {
        store
                .onAdd((bucket) -> {
                    bucket.loadDictionaries(this);
                });
    }

    void decorateCBlockStore(IdentifiableStore<CBlock> baseStoreCreator) {
        // Nothing to decorate
    }

    @Override
    public void addDictionary(Dictionary dict) {
        dictionaries.add(dict);
    }

    @Override
    public Dictionary getDictionary(DictionaryId id) {
        return dictionaries.get(id);
    }

    @Override
    public void updateDictionary(Dictionary dict) {
        dictionaries.update(dict);
    }

    @Override
    public void removeDictionary(DictionaryId id) {
        dictionaries.remove(id);
    }

    @Override
    public EncodedDictionary getPrimaryDictionary() {
        return new EncodedDictionary(dictionaries.get(ConqueryConstants.getPrimaryDictionary(getDataset())), StringTypeEncoded.Encoding.UTF8);
    }

    @Override
    public void addImport(Import imp) {
        imports.add(imp);
    }

    @Override
    public Import getImport(ImportId id) {
        return imports.get(id);
    }

    @Override
    public Collection<Import> getAllImports() {
        return imports.getAll();
    }

    @Override
    public void updateImport(Import imp) {
        imports.update(imp);
    }

    @Override
    public void removeImport(ImportId id) {
        imports.remove(id);
    }

    @Override
    public Dataset getDataset() {
        return dataset.get();
    }

    @Override
    public void updateDataset(Dataset dataset) {
        this.dataset.update(dataset);
    }

    @Override
    public List<Table> getTables() {
        return new ArrayList<>(tables.getAll());
    }

    @Override
    public Table getTable(TableId tableId) {
        return tables.get(tableId);
    }

    @Override
    public void addTable(Table table) {
        tables.add(table);
    }

    @Override
    public void removeTable(TableId table) {
        tables.remove(table);
    }

    @Override
    public List<SecondaryIdDescription> getSecondaryIds() {
        return new ArrayList<>(secondaryIds.getAll());
    }

    @Override
    public SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId) {
        return secondaryIds.get(descriptionId);
    }

    @Override
    public void addSecondaryId(SecondaryIdDescription secondaryIdDescription) {
        secondaryIds.add(secondaryIdDescription);
    }

    @Override
    public void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId) {
        secondaryIds.remove(secondaryIdDescriptionId);
    }

    @Override
    public Concept<?> getConcept(ConceptId id) {
        return concepts.get(id);
    }

    @Override
    public boolean hasConcept(ConceptId id) {
        return concepts.get(id) != null;
    }

    @Override
    @SneakyThrows
    public void updateConcept(Concept<?> concept) {
        concepts.update(concept);
    }

    @Override
    public void removeConcept(ConceptId id) {
        concepts.remove(id);
    }

    @Override
    public Collection<? extends Concept<?>> getAllConcepts() {
        return concepts.getAll();
    }

    @Override
    public void close() throws IOException {

    }
}
