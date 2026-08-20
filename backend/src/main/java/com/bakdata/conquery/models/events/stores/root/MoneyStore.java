package com.bakdata.conquery.models.events.stores.root;


import java.math.BigDecimal;

import com.bakdata.conquery.models.events.MajorTypeId;

/**
 * {@link ColumnStore} for Money based values.
 * See also {@link MajorTypeId#MONEY} and {@link com.bakdata.conquery.models.preproc.parser.specific.MoneyParser}.
 */
public interface MoneyStore extends ColumnStore {

	BigDecimal getMoney(int event);
	void setMoney(int event, BigDecimal money);

	@Override
	default Object createScriptValue(int event) {
		return getMoney(event);
	}
}
