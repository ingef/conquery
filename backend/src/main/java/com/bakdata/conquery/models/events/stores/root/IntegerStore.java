package com.bakdata.conquery.models.events.stores.root;


import com.bakdata.conquery.models.events.MajorTypeId;

/**
 * {@link ColumnStore} for Integer based values.
 * <p>
 * See also {@link MajorTypeId#INTEGER} and {@link com.bakdata.conquery.models.preproc.parser.specific.IntegerParser}.
 */
public interface IntegerStore extends ColumnStore {

	void setInteger(int event, long value);

	@Override
	default Object createScriptValue(int event) {
		return getInteger(event);
	}

	long getInteger(int event);
}
