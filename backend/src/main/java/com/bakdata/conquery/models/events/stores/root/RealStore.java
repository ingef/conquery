package com.bakdata.conquery.models.events.stores.root;

/**
 * {@link ColumnStore} for real values (double/float).
 *
 * See also {@link com.bakdata.conquery.models.events.parser.MajorTypeId#REAL} and {@link com.bakdata.conquery.models.events.parser.specific.RealParser}.
 */
public interface RealStore extends ColumnStore {

	double getReal(int event);
	void setReal(int event, double value);

	@Override
	default Object createScriptValue(int event) {
		return getReal(event);
	}
}
