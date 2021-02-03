package com.bakdata.conquery.models.events.stores.root;


/**
 * {@link ColumnStore} for Integer based values.
 * <p>
 * See also {@link com.bakdata.conquery.models.events.parser.MajorTypeId#INTEGER} and {@link com.bakdata.conquery.models.events.parser.specific.IntegerParser}.
 */
public interface IntegerStore extends ColumnStore {

	void setInteger(int event, long value);

	@Override
	default Object createScriptValue(int event) {
		return getInteger(event);
	}

	long getInteger(int event);
}
