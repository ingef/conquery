package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.models.events.MajorTypeId;

/**
 * {@link ColumnStore} for boolean values.
 *
 * See also {@link MajorTypeId#BOOLEAN} and {@link com.bakdata.conquery.models.preproc.parser.specific.BooleanParser}.
 *
 * @implSpec this class cannot handle null values.
 */
public interface BooleanStore extends ColumnStore {

	boolean getBoolean(int event);
	void setBoolean(int event, boolean value);

	@Override
	default void setNull(int event){
		throw new IllegalStateException("BooleanStore cannot have Null values.");
	}

	@Override
	default Object createScriptValue(int event) {
		return getBoolean(event);
	}
}
