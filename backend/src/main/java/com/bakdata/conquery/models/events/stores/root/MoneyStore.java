package com.bakdata.conquery.models.events.stores.root;


import com.bakdata.conquery.models.events.MajorTypeId;

/**
 * {@link ColumnStore} for Money based values.
 * See also {@link MajorTypeId#MONEY} and {@link com.bakdata.conquery.models.preproc.parser.specific.MoneyParser}.
 */
public interface MoneyStore extends ColumnStore {

	long getMoney(int event);
	void setMoney(int event, long money);

	@Override
	default Object createScriptValue(int event) {
		return getMoney(event);
	}
}
