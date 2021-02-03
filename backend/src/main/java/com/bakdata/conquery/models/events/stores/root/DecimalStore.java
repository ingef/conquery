package com.bakdata.conquery.models.events.stores.root;

import java.math.BigDecimal;

/**
 * {@link ColumnStore}  for {@link BigDecimal} values.
 *
 * See also {@link com.bakdata.conquery.models.events.parser.MajorTypeId#DECIMAL} and {@link com.bakdata.conquery.models.events.parser.specific.DecimalParser}.
 */
public interface DecimalStore extends ColumnStore {

	BigDecimal getDecimal(int event);
	void setDecimal(int event, BigDecimal value);

	@Override
	default Object createScriptValue(int event) {
		return getDecimal(event);
	}
}
