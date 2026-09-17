package com.bakdata.conquery.models.events.stores.root;

import java.math.BigDecimal;

import com.bakdata.conquery.models.events.MajorTypeId;

/**
 * {@link ColumnStore}  for {@link BigDecimal} values.
 *
 * See also {@link MajorTypeId#DECIMAL} and {@link com.bakdata.conquery.models.preproc.parser.specific.DecimalParser}.
 */
public interface DecimalStore extends ColumnStore {

	BigDecimal getDecimal(int event);
	void setDecimal(int event, BigDecimal value);

	@Override
	default Object createScriptValue(int event) {
		return getDecimal(event);
	}
}
