package com.bakdata.conquery.models.types.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.date.DateRangeStore;
import com.bakdata.conquery.models.events.stores.date.PackedDateRangeStore;
import com.bakdata.conquery.models.events.stores.date.QuarterDateStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.daterange.DateRangeTypeDateRange;
import com.bakdata.conquery.models.types.specific.daterange.DateRangeTypePacked;
import com.bakdata.conquery.models.types.specific.daterange.DateRangeTypeQuarter;
import com.bakdata.conquery.util.DateFormats;
import com.bakdata.conquery.util.PackedUnsigned1616;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@ToString(callSuper = true)
public class DateRangeParser extends Parser<CDateRange> {

	private boolean onlyQuarters = true;
	private boolean anyOpen = false;
	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;

	public DateRangeParser(ParserConfig config) {

	}

	@Override
	protected CDateRange parseValue(@Nonnull String value) throws ParsingException {
		return DateRangeParser.parseISORange(value);
	}

	@Override
	protected void registerValue(CDateRange v) {
		onlyQuarters = onlyQuarters && v.isSingleQuarter();
		anyOpen = anyOpen || v.isOpen();

		if (!anyOpen) {
			maxValue = Math.max(maxValue, v.getMaxValue());
			minValue = Math.min(minValue, v.getMinValue());
		}
	}

	public static CDateRange parseISORange(String value) throws ParsingException {
		if (value == null) {
			return null;
		}
		String[] parts = StringUtils.split(value, '/');
		if (parts.length != 2) {
			throw ParsingException.of(value, "daterange");
		}

		return CDateRange.of(
				DateFormats.parseToLocalDate(parts[0]),
				DateFormats.parseToLocalDate(parts[1])
		);
	}

	@Override
	protected CType<CDateRange> decideType() {
		// We cannot yet do meaningful compression for open dateranges.
		// TODO: 27.04.2020 consider packed compression with extra value as null value.
		if (anyOpen) {
			return new DateRangeTypeDateRange(DateRangeStore.create(getLines()))
			;
		}

		if (onlyQuarters) {
			final IntegerParser quarterParser = new IntegerParser();
			quarterParser.setLines(getLines());
			quarterParser.setMaxValue(maxValue);
			quarterParser.setMinValue(minValue);

			return new DateRangeTypeQuarter(new QuarterDateStore(quarterParser.decideType()));
		}
		// min or max can be Integer.MIN/MAX_VALUE when this happens, the left expression overflows causing it to be true when it is not.
		// We allow this exception to happen as it would imply erroneous data.
		if (Math.subtractExact(maxValue, minValue) < PackedUnsigned1616.MAX_VALUE) {
			log.debug("Decided for Packed: min={}, max={}", minValue, maxValue);

			final IntegerParser quarterParser = new IntegerParser();
			quarterParser.setLines(getLines());
			quarterParser.setMaxValue(PackedUnsigned1616.pack(maxValue, maxValue));
			quarterParser.setMinValue(PackedUnsigned1616.pack(minValue, minValue));

			return new DateRangeTypePacked(minValue, maxValue, new PackedDateRangeStore(quarterParser.decideType()));
		}

		return new DateRangeTypeDateRange(DateRangeStore.create(getLines()));
	}
}
