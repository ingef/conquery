package com.bakdata.conquery.models.events.stores.root;

/**
 * {@link ColumnStore} for boolean values.
 *
 * See also {@link com.bakdata.conquery.models.events.parser.MajorTypeId#BOOLEAN} and {@link com.bakdata.conquery.models.events.parser.specific.BooleanParser}.
 */
public interface BooleanStore extends ColumnStore {

	boolean getBoolean(int event);
	void setBoolean(int event, boolean value);

	@Override
	default Object createScriptValue(int event) {
		return getBoolean(event);
	}
}
