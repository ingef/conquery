package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Every implementation must guarantee IDs between 0 and size.
 *
 */
public interface StringStore extends Iterable<String>, ColumnStore {


	int getString(int event);
	void setString(int event, int value);

	String getElement(int id);
	
	int size();

	int getId(String value);
	
	@JsonIgnore
	Dictionary getUnderlyingDictionary();

	void setUnderlyingDictionary(DictionaryId newDict);

	/**
	 * After applying DictionaryMapping a new store might be needed.
	 */
	void setIndexStore(IntegerStore newType);

	default void loadDictionaries(NamespacedStorage storage) {}
}
