package com.bakdata.conquery.models.events.stores.root;

import java.util.stream.Stream;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ColumnStore} for dictionary encoded string values.
 * <p>
 * See also {@link MajorTypeId#STRING} and {@link com.bakdata.conquery.models.preproc.parser.specific.StringParser}.
 * <p>
 * This class has a lot of special methods for handling dictionary encoded values.
 *
 * @implSpec Every implementation must guarantee IDs between 0 and size.
 */
public interface StringStore extends ColumnStore {

	static final Logger log = LoggerFactory.getLogger(StringStore.class);


	int getString(int event);

	void setString(int event, int value);

	String getElement(int id);

	/**
	 * Maximum number of distinct values in this Store.
	 */
	int size();


	Stream<String> iterateValues();

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
