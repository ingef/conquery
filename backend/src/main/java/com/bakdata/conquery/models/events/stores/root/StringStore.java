package com.bakdata.conquery.models.events.stores.root;

import java.util.stream.Stream;

public interface StringStore extends ColumnStore {

	String getString(int event);

	void setString(int event, String value);

	/**
	 * Maximum number of distinct values in this Store.
	 */
	int size();

	Stream<String> streamValues();

}
