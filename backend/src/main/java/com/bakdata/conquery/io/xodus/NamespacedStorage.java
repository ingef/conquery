package com.bakdata.conquery.io.xodus;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;

public interface NamespacedStorage extends ConqueryStorage {
	
	void addDictionary(Dictionary dict);
	Dictionary getDictionary(DictionaryId id);
	void updateDictionary(Dictionary dict);
	void removeDictionary(DictionaryId id);
	Dictionary computeDictionary(DictionaryId id); // todo inline this
	EncodedDictionary getPrimaryDictionary();
	
	void addImport(Import imp);
	Import getImport(ImportId id);
	Collection<Import> getAllImports();
	void updateImport(Import imp);
	void removeImport(ImportId id);
	
	Dataset getDataset();
	void updateDataset(Dataset dataset);

	List<Table> getTables();
	Table getTable(TableId tableId);
	void addTable(Table table);
	void removeTable(TableId table);

	List<SecondaryIdDescription> getSecondaryIds();
	SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId);
	void addSecondaryId(SecondaryIdDescription secondaryIdDescription);
	void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId);



	Concept<?> getConcept(ConceptId id);
	boolean hasConcept(ConceptId id);
	void updateConcept(Concept<?> concept);
	void removeConcept(ConceptId id);
	Collection<? extends Concept<?>> getAllConcepts();
}
