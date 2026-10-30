package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;

@ToString(callSuper = true)
public class DateParser extends Parser<Integer, DateStore, IntegerColumnValues> {

	private final IntegerStoreSelector selector = new IntegerStoreSelector();

	public DateParser(ConqueryConfig config) {
		super(config, new IntegerColumnValues());
	}

	public void addDate(int value) {
		recordLine();
		selector.register(value);
		getColumnValues().addInt(value);
	}

	public int getDate(int event) {
		return getColumnValues().getInt(event);
	}

	protected DateStore decideType() {
		return decideType(getLines());
	}

	@Override
	protected DateStore decideType(int storeLines) {
		IntegerStore subDecision = selector.decideType(storeLines);
		return new IntegerDateStore(subDecision);
	}

	@Override
	public void setValue(DateStore store, int event, Integer value) {
		store.setDate(event, value);
	}

}
