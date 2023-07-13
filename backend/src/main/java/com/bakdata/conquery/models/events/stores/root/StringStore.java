package com.bakdata.conquery.models.events.stores.root;

import java.util.stream.Stream;

import com.bakdata.conquery.models.events.MajorTypeId;

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

	String getString(int event);

	void setString(int event, String value);

	/**
	 * Maximum number of distinct values in this Store.
	 */
	int size();


	Stream<String> iterateValues();

}
