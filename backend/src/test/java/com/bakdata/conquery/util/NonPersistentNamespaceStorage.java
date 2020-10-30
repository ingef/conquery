package com.bakdata.conquery.util;

import java.io.IOException;
import java.util.Collection;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import lombok.NonNull;

public class NonPersistentNamespaceStorage implements NamespaceStorage {
	
	private Dataset dataset;

	@Override
	public void addDictionary(Dictionary dict) {
		throw new UnsupportedOperationException();

	}

	@Override
	public Dictionary getDictionary(DictionaryId id) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateDictionary(Dictionary dict) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void removeDictionary(DictionaryId id) {
		throw new UnsupportedOperationException();

	}

	@Override
	public Dictionary computeDictionary(DictionaryId id) {
		throw new UnsupportedOperationException();

	}

	@Override
	public DirectDictionary getPrimaryDictionary() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void addImport(Import imp) {
		throw new UnsupportedOperationException();

	}

	@Override
	public Import getImport(ImportId id) {
		throw new UnsupportedOperationException();

	}

	@Override
	public Collection<Import> getAllImports() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateImport(Import imp) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void removeImport(ImportId id) {
		throw new UnsupportedOperationException();

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
	public Concept<?> getConcept(ConceptId id) {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean hasConcept(ConceptId id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateConcept(Concept<?> concept) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void removeConcept(ConceptId id) {
		throw new UnsupportedOperationException();

	}

	@Override
	public Collection<? extends Concept<?>> getAllConcepts() {
		throw new UnsupportedOperationException();

	}

	@Override
	public Validator getValidator() {
		throw new UnsupportedOperationException();

	}

	@Override
	public CentralRegistry getCentralRegistry() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void loadData() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();

	}

	@Override
	public String getStorageOrigin() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void close() throws IOException {
		throw new UnsupportedOperationException();

	}

	@Override
	public MetaStorage getMetaStorage() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void setMetaStorage(@NonNull MetaStorage storage) {
		throw new UnsupportedOperationException();

	}

	@Override
	public StructureNode[] getStructure() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateStructure(StructureNode[] structure) throws JSONException {
		throw new UnsupportedOperationException();

	}

	@Override
	public PersistentIdMap getIdMapping() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateIdMapping(PersistentIdMap idMap) throws JSONException {
		throw new UnsupportedOperationException();

	}

}
