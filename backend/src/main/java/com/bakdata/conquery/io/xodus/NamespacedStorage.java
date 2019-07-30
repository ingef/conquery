package com.bakdata.conquery.io.xodus;

import java.util.Collection;
import java.util.NoSuchElementException;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;

public interface NamespacedStorage extends ConqueryStorage {
	
	void addDictionary(Dictionary dict) throws JSONException;
	Dictionary getDictionary(DictionaryId id) throws NoSuchElementException;
	void updateDictionary(Dictionary dict) throws JSONException;
	void removeDictionary(DictionaryId id) throws NoSuchElementException;
	Dictionary computeDictionary(DictionaryId id) throws JSONException;
	DirectDictionary getPrimaryDictionary();
	
	void addImport(Import imp) throws JSONException;
	Import getImport(ImportId id) throws NoSuchElementException;
	Collection<Import> getAllImports();
	void updateImport(Import imp) throws JSONException;
	void removeImport(ImportId id) throws NoSuchElementException;
	
	Dataset getDataset();
	void updateDataset(Dataset dataset) throws JSONException;
	
	Concept<?> getConcept(ConceptId id) throws NoSuchElementException;
	void updateConcept(Concept<?> concept) throws JSONException;
	void removeConcept(ConceptId id) throws NoSuchElementException;
	Collection<? extends Concept<?>> getAllConcepts();
}
