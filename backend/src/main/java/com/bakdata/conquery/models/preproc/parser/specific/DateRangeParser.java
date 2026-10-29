package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.Objects;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.specific.DirectDateRangeStore;
import com.bakdata.conquery.models.events.stores.specific.QuarterDateRangeStore;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
public class DateRangeParser extends Parser<CDateRange, DateRangeStore, DateRangeColumnValues> {

	private final IntegerStoreSelector minSelector = new IntegerStoreSelector();
	private final IntegerStoreSelector maxSelector = new IntegerStoreSelector();
	private final IntegerStoreSelector quarterSelector = new IntegerStoreSelector();

	private boolean onlyQuarters = true;
	private boolean anyOpen;

	public DateRangeParser(ConqueryConfig config) {
		super(config, new DateRangeColumnValues());
	}

	public void addDateRange(CDateRange value) {
		Objects.requireNonNull(value, "value");
		recordLine();
		onlyQuarters = onlyQuarters && value.isSingleQuarter();

		anyOpen = anyOpen || value.isOpen();

		if (value.hasUpperBound()) {
			maxSelector.register(value.getMaxValue());
			quarterSelector.register(value.getMaxValue());
		}
		if (value.hasLowerBound()) {
			minSelector.register(value.getMinValue());
			quarterSelector.register(value.getMinValue());
		}

		getColumnValues().addDateRange(value);
	}

	protected DateRangeStore decideType() {
		return decideType(getLines());
	}

	@Override
	protected DateRangeStore decideType(int storeLines) {

		// Quarters cannot encode open ranges.
		if (!anyOpen && onlyQuarters) {
			return new QuarterDateRangeStore(quarterSelector.decideType(storeLines));
		}

		return new DirectDateRangeStore(decideDateType(minSelector, storeLines), decideDateType(maxSelector, storeLines));
	}

	private static DateStore decideDateType(IntegerStoreSelector selector, int storeLines) {
		if (selector.isEmpty()) {
			return EmptyStore.INSTANCE;
		}
		return new IntegerDateStore(selector.decideType(storeLines));
	}

	@Override
	public void setValue(DateRangeStore store, int event, CDateRange value) {
		store.setDateRange(event, value);
	}

}
