package com.bakdata.conquery.util;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import lombok.Getter;
import lombok.NonNull;

import javax.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NonPersistentNamespaceStorage implements NamespaceStorage {

    @Getter
    private final Validator validator;

    private MetaStorage metaStorage;
    private StructureNode[] structureNodes;
    private PersistentIdMap persistentIdMap;
    private WorkerToBucketsMap workerToBucketsMap;
    private IdMap<DictionaryId, Dictionary> dictionaries = new IdMap<>();
    private IdMap<ImportId, Import> imports = new IdMap<>();
    private Dataset dataset;
    private IdMap<TableId, Table> tables = new IdMap<>();
    private IdMap<SecondaryIdDescriptionId, SecondaryIdDescription> secondaryIds = new IdMap<>();
    private IdMap<ConceptId, Concept<?>> concepts = new IdMap<>();

    public NonPersistentNamespaceStorage(Validator validator) {
        this.validator = validator;
    }

    @Override
    public CentralRegistry getCentralRegistry() {
        return null;
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
    public MetaStorage getMetaStorage() {
        return metaStorage;
    }

    @Override
    public void setMetaStorage(@NonNull MetaStorage storage) {
        this.metaStorage = metaStorage;
    }

    @Override
    public StructureNode[] getStructure() {
        return structureNodes;
    }

    @Override
    public void updateStructure(StructureNode[] structure) throws JSONException {
        this.structureNodes = structure;
    }

    @Override
    public PersistentIdMap getIdMapping() {
        return persistentIdMap;
    }

    @Override
    public void updateIdMapping(PersistentIdMap idMap) throws JSONException {
        this.persistentIdMap = idMap;
    }

    @Override
    public void setWorkerToBucketsMap(WorkerToBucketsMap map) {
        this.workerToBucketsMap = workerToBucketsMap;
    }

    @Override
    public WorkerToBucketsMap getWorkerBuckets() {
        return workerToBucketsMap;
    }

    @Override
    public void addDictionary(Dictionary dict) {
        dictionaries.add(dict);
    }

    @Override
    public Dictionary getDictionary(DictionaryId id) {
        return dictionaries.getOrFail(id);
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
        this.dataset = dataset;
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
    }

    @Override
    public void removeTable(TableId table) {
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
        return concepts.containsKey(id);
    }

    @Override
    public void updateConcept(Concept<?> concept) {
        concepts.update(concept);
    }

    @Override
    public void removeConcept(ConceptId id) {
        concepts.remove(id)
    }

    @Override
    public Collection<? extends Concept<?>> getAllConcepts() {
        return concepts.values();
    }

    @Override
    public void close() throws IOException {

    }
}
