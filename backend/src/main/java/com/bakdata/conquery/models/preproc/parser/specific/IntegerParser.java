package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;

@ToString(callSuper = true)
public class IntegerParser extends Parser<Long, IntegerStore, LongColumnValues> {

	private final IntegerStoreSelector selector = new IntegerStoreSelector();

	public IntegerParser(ConqueryConfig config) {
		super(config, new LongColumnValues());
	}


	public void addLong(long value) {
		recordLine();
		selector.register(value);
		getColumnValues().addLong(value);
	}

	@Override
	protected IntegerStore decideType(int storeLines) {
		return selector.decideType(storeLines);
	}

	@Override
	public void setValue(IntegerStore store, int event, Long value) {
		store.setInteger(event, value);
	}

}
