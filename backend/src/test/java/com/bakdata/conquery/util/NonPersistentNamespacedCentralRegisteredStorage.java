package com.bakdata.conquery.util;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.*;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import lombok.Getter;
import lombok.SneakyThrows;

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

    private IdMap<DictionaryId, Dictionary> dictionaries = new IdMap<>();
    private IdMap<ImportId, Import> imports = new IdMap<>();
    private Dataset dataset;
    private IdMap<TableId, Table> tables = new IdMap<>();
    private IdMap<SecondaryIdDescriptionId, SecondaryIdDescription> secondaryIds = new IdMap<>();
    private IdMap<ConceptId, Concept<?>> concepts = new IdMap<>();

    public NonPersistentNamespacedCentralRegisteredStorage(Validator validator) {
        this.validator = validator;
    }


    @Override
    public CentralRegistry getCentralRegistry() {
        return centralRegistry;
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
        centralRegistry.register(dict);
    }

    @Override
    public Dictionary getDictionary(DictionaryId id) {
        return dictionaries.get(id);
    }

    @Override
    public void updateDictionary(Dictionary dict) {
        centralRegistry.remove(dict);
        dictionaries.update(dict);
        centralRegistry.register(dict);
    }

    @Override
    public void removeDictionary(DictionaryId id) {
        centralRegistry.remove(id);
        dictionaries.remove(id);
    }

    @Override
    public Dictionary computeDictionary(DictionaryId id) {
        Dictionary e = getDictionary(id);
        if (e == null) {
            e = new MapDictionary(id);
            updateDictionary(e);
        }
        return e;
    }

    @Override
    public DirectDictionary getPrimaryDictionary() {
        return new DirectDictionary(dictionaries.get(ConqueryConstants.getPrimaryDictionary(getDataset())));
    }

    @Override
    public void addImport(Import imp) {
        imports.add(imp);
        imp.loadExternalInfos(this);

        for (Concept<?> c : getAllConcepts()) {
            for (Connector con : c.getConnectors()) {
                if (con.getTable().getId().equals(imp.getTable())) {
                    con.addImport(imp);
                }
            }
        }
    }

    @Override
    public Import getImport(ImportId id) {
        return imports.get(id);
    }

    @Override
    public Collection<Import> getAllImports() {
        return imports.values();
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
        return dataset;
    }

    @Override
    public void updateDataset(Dataset dataset) {
        centralRegistry.remove(dataset);
        this.dataset = dataset;
        centralRegistry.register(dataset);
    }

    @Override
    public List<Table> getTables() {
        return new ArrayList<>(tables.values());
    }

    @Override
    public Table getTable(TableId tableId) {
        return tables.get(tableId);
    }

    @Override
    public void addTable(Table table) {
        tables.add(table);
        centralRegistry.register(table);

        for (Column c : table.getColumns()) {
            centralRegistry.register(c);
        }
    }

    @Override
    public void removeTable(TableId table) {
        for (Column c : tables.get(table).getColumns()) {
            centralRegistry.remove(c);
        }
        centralRegistry.remove(table);

        tables.remove(table);
    }

    @Override
    public List<SecondaryIdDescription> getSecondaryIds() {
        return new ArrayList<>(secondaryIds.values());
    }

    @Override
    public SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId) {
        return secondaryIds.get(descriptionId);
    }

    @Override
    public void addSecondaryId(SecondaryIdDescription secondaryIdDescription) {
        secondaryIds.add(secondaryIdDescription);
        centralRegistry.register(secondaryIdDescription);
    }

    @Override
    public void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId) {
        centralRegistry.remove(secondaryIdDescriptionId);
        secondaryIds.remove(secondaryIdDescriptionId);
    }

    @Override
    public Concept<?> getConcept(ConceptId id) {
        return concepts.get(id);
    }

    @Override
    public boolean hasConcept(ConceptId id) {
        return concepts.containsKey(id);
    }

    @Override
    @SneakyThrows
    public void updateConcept(Concept<?> concept) {
        centralRegistry.remove(concept);

        concept.initElements(validator);
        concepts.update(concept);
        centralRegistry.register(concept);
        concept.getSelects().forEach(centralRegistry::register);
        for (Connector c : concept.getConnectors()) {
            centralRegistry.register(c);
            c.collectAllFilters().forEach(centralRegistry::register);
            c.getSelects().forEach(centralRegistry::register);
        }
        //add imports of table
        for (Import imp : getAllImports()) {
            for (Connector con : concept.getConnectors()) {
                if (con.getTable().getId().equals(imp.getTable())) {
                    con.addImport(imp);
                }
            }
        }
    }

    @Override
    public void removeConcept(ConceptId id) {
        centralRegistry.remove(id);
        Concept<?> concept = concepts.get(id);
        concept.getSelects().forEach(centralRegistry::remove);
        //see #146  remove from Dataset.concepts
        for (Connector c : concept.getConnectors()) {
            c.getSelects().forEach(centralRegistry::remove);
            c.collectAllFilters().stream().map(Filter::getId).forEach(centralRegistry::remove);
            centralRegistry.remove(c.getId());
        }
        concepts.remove(id);
    }

    @Override
    public Collection<? extends Concept<?>> getAllConcepts() {
        return concepts.values();
    }

    @Override
    public void close() throws IOException {

    }
}
