package com.bakdata.conquery.io.xodus;

import java.util.Collection;

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
	Dictionary getDictionary(DictionaryId id);
	void updateDictionary(Dictionary dict) throws JSONException;
	void removeDictionary(DictionaryId id);
	Dictionary computeDictionary(DictionaryId id) throws JSONException;
	DirectDictionary getPrimaryDictionary();
	
	void addImport(Import imp) throws JSONException;
	Import getImport(ImportId id);
	Collection<Import> getAllImports();
	void updateImport(Import imp) throws JSONException;
	void removeImport(ImportId id);
	
	Dataset getDataset();
	void updateDataset(Dataset dataset);
	
	Concept<?> getConcept(ConceptId id);
	boolean hasConcept(ConceptId id);
	void updateConcept(Concept<?> concept) throws JSONException;
	void removeConcept(ConceptId id);
	Collection<? extends Concept<?>> getAllConcepts();
}
