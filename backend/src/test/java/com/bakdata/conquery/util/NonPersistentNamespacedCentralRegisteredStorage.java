package com.bakdata.conquery.util;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.io.xodus.StoreInfo;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.*;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.esf.OtherHash;

import javax.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class NonPersistentNamespacedCentralRegisteredStorage implements NamespacedStorage {

    @Getter
    private final Validator validator;
    @Getter
    private final CentralRegistry centralRegistry = new CentralRegistry();

    protected SingletonStore<Dataset> dataset = createDatasetStore((storeId) -> new NonPersistentStore());
    protected IdentifiableStore<SecondaryIdDescription> secondaryIds = createSecondaryIdDescriptionStore((storeId) -> new NonPersistentStore());
    protected KeyIncludingStore<IId<Dictionary>, Dictionary> dictionaries = StoreInfo.DICTIONARIES.identifiable(new NonPersistentStore(), centralRegistry);
    protected IdentifiableStore<Import> imports = createImportStore((storeId) -> new NonPersistentStore());
    protected IdentifiableStore<Table> tables = createTableStore((storeId) -> new NonPersistentStore());
    protected IdentifiableStore<Concept<?>> concepts = createConceptStore((storeId) -> new NonPersistentStore());

    public NonPersistentNamespacedCentralRegisteredStorage(Validator validator) {
        this.validator = validator;
    }

    @Override
    public void loadData() {

    }

    @Override
    public void clear() {

    }

    @Override
    public void remove() {

    }

    @Override
    public String getStorageOrigin() {
        return null;
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
