package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link ColumnStore} for dictionary encoded string values.
 *
 * See also {@link com.bakdata.conquery.models.events.parser.MajorTypeId#STRING} and {@link com.bakdata.conquery.models.events.parser.specific.StringParser}.
 *
 * This class has a lot of special methods for handling dictionary encoded values.
 *
 * @implSpec Every implementation must guarantee IDs between 0 and size.
 *
 */
public interface StringStore extends Iterable<String>, ColumnStore {


	int getString(int event);
	void setString(int event, int value);

	String getElement(int id);

	/**
	 * Number of distinct values in this Store.
	 */
	int size();

	/**
	 * Lookup the id of a value in the dictionary.
	 */
	int getId(String value);



	/**
	 * Allows injecting a different dictionary (usually a shared dictionary)
	 */
	void setUnderlyingDictionary(DictionaryId newDict);

	@JsonIgnore
	Dictionary getUnderlyingDictionary();

	@JsonIgnore
	boolean isDictionaryHolding();

	/**
	 * Loads all dictionaries.
	 *
	 * TODO: This can be replaced with {@link com.bakdata.conquery.io.jackson.serializer.NsIdRef} when used properly.
	 */
	default void loadDictionaries(NamespacedStorage storage) {}


	/**
	 * After applying DictionaryMapping a new store might be needed.
	 */
	void setIndexStore(IntegerStore newType);


}
