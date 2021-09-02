package com.bakdata.conquery.models.preproc.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeDateRange;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeQuarter;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.DateReader;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@ToString(callSuper = true)
public class DateRangeParser extends Parser<CDateRange, DateRangeStore> {

	private final DateParser minParser;
	private final DateParser maxParser;
	private final DateReader dateReader;

	private boolean onlyQuarters = true;
	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;
	private boolean anyOpen;

	public DateRangeParser(ConqueryConfig config) {
		super(config);
		minParser = new DateParser(config);
		maxParser = new DateParser(config);
		dateReader = config.getLocale().getDateReader();
	}

	@Override
	protected CDateRange parseValue(@Nonnull String value) {
		return dateReader.parseToCDateRange(value);
	}

	@Override
	protected void registerValue(CDateRange v) {
		onlyQuarters = onlyQuarters && v.isSingleQuarter();

		anyOpen = anyOpen || v.isOpen();

		if (v.hasUpperBound()) {
			maxValue = Math.max(maxValue, v.getMaxValue());
			maxParser.addLine(v.getMaxValue());
		}
		if (v.hasLowerBound()) {
			minValue = Math.min(minValue, v.getMinValue());
			minParser.addLine(v.getMinValue());
		}
	}

	@Override
	protected DateRangeStore decideType() {

		// Quarters cannot encode open ranges.
		if (!anyOpen && onlyQuarters) {
			final IntegerParser quarterParser = new IntegerParser(getConfig());
			quarterParser.setLines(getLines());
			quarterParser.setMaxValue(maxValue);
			quarterParser.setMinValue(minValue);

			return new DateRangeTypeQuarter(quarterParser.findBestType());
		}


		// They need to be aligned if they are non-empty.

		if(!minParser.isEmpty()) {
			minParser.setLines(getLines());
		}

		if(!maxParser.isEmpty()){
			maxParser.setLines(getLines());
		}

		return new DateRangeTypeDateRange(minParser.findBestType(), maxParser.findBestType());
	}

	@Override
	public void setValue(DateRangeStore store, int event, CDateRange value) {
		store.setDateRange(event, value);
	}

	@Override
	public ColumnValues<CDateRange> createColumnValues() {
		return new ListColumnValues<>();
	}

}
