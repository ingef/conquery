package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.models.events.MajorTypeId;

/**
 * {@link ColumnStore} for real values (double/float).
 *
 * See also {@link MajorTypeId#REAL} and {@link com.bakdata.conquery.models.preproc.parser.specific.RealParser}.
 */
public interface RealStore extends ColumnStore {

	double getReal(int event);
	void setReal(int event, double value);

	@Override
	default Object createScriptValue(int event) {
		return getReal(event);
	}
}
