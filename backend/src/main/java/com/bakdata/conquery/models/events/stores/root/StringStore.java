package com.bakdata.conquery.models.events.stores.root;

import java.util.Iterator;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ColumnStore} for dictionary encoded string values.
 *
 * See also {@link MajorTypeId#STRING} and {@link com.bakdata.conquery.models.preproc.parser.specific.StringParser}.
 *
 * This class has a lot of special methods for handling dictionary encoded values.
 *
 * @implSpec Every implementation must guarantee IDs between 0 and size.
 *
 */
public interface StringStore extends ColumnStore {


	int getString(int event);
	void setString(int event, int value);

	String getElement(int id);

	/**
	 * Number of distinct values in this Store.
	 */
	int size();

	@NotNull
	Iterator<String> iteratorForLines(long lines);

	/**
	 * Lookup the id of a value in the dictionary.
	 */
	int getId(String value);


	@JsonIgnore
	Dictionary getUnderlyingDictionary();
	@JsonIgnore
	void setUnderlyingDictionary(Dictionary dictionary);

	@JsonIgnore
	boolean isDictionaryHolding();



	/**
	 * After applying DictionaryMapping a new store might be needed.
	 */
	void setIndexStore(IntegerStore newType);

}
