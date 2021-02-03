package com.bakdata.conquery.models.events.stores.root;


/**
 * {@link ColumnStore} for Money based values.
 * See also {@link com.bakdata.conquery.models.events.parser.MajorTypeId#MONEY} and {@link com.bakdata.conquery.models.events.parser.specific.MoneyParser}.
 */
public interface MoneyStore extends ColumnStore {

	long getMoney(int event);
	void setMoney(int event, long money);

	@Override
	default Object createScriptValue(int event) {
		return getMoney(event);
	}
}
