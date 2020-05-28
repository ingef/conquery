package com.bakdata.conquery.models.types.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.NoopTransformer;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.specific.DateRangeTypeDateRange;
import com.bakdata.conquery.models.types.specific.DateRangeTypePacked;
import com.bakdata.conquery.models.types.specific.DateRangeTypeQuarter;
import com.bakdata.conquery.util.DateFormats;
import com.bakdata.conquery.util.PackedUnsigned1616;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@ToString(callSuper = true)
public class DateRangeParser extends Parser<CDateRange> {

	private boolean onlyQuarters = true;
	private boolean onlyClosed = true;
	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;
	
	@Override
	protected CDateRange parseValue(@Nonnull String value) throws ParsingException {
		return DateRangeParser.parseISORange(value);
	}
	
	@Override
	protected void registerValue(CDateRange v) {
		// test if value is already set to avoid expensive computation.
		if(onlyQuarters && !v.isSingleQuarter()) {
			onlyQuarters = false;
		}

		if (onlyClosed && v.isOpen()) {
			onlyClosed = false;
		}

		if(v.getMaxValue() > maxValue) {
			maxValue = v.getMaxValue();
		}

		if(v.getMinValue() < minValue) {
			minValue = v.getMinValue();
		}
	}

	public static CDateRange parseISORange(String value) throws ParsingException {
		if(value==null) {
			return null;
		}
		String[] parts = StringUtils.split(value, '/');
		if(parts.length!=2) {
			throw ParsingException.of(value, "daterange");
		}

		return CDateRange.of(
				DateFormats.parseToLocalDate(parts[0]),
				DateFormats.parseToLocalDate(parts[1])
		);
	}
	
	@Override
	protected Decision<CDateRange, ?, ? extends CType<CDateRange, ?>> decideType() {
		// We cannot yet do meaningful compression for open dateranges.
		// TODO: 27.04.2020 consider packed compression with extra value as null value.
		if(!onlyClosed) {
			return new Decision<>(
					new NoopTransformer<>(),
					new DateRangeTypeDateRange()
			);
		}

		if(onlyQuarters) {
			DateRangeTypeQuarter type = new DateRangeTypeQuarter();
			return new Decision<>(
				new Transformer<CDateRange, Integer>() {
					@Override
					public Integer transform(CDateRange value) {
						return value.getMinValue();
					}
				},
				type
			);
		}
		// min or max can be Integer.MIN/MAX_VALUE when this happens, the left expression overflows causing it to be true when it is not.
		// We allow this exception to happen as it would imply erroneous data.
		if (Math.subtractExact(maxValue, minValue) < PackedUnsigned1616.MAX_VALUE) {
			DateRangeTypePacked type = new DateRangeTypePacked();
			type.setMinValue(minValue);
			type.setMaxValue(maxValue);

			log.debug("Decided for Packed: min={}, max={}", minValue, maxValue);

			return new Decision<>(
					new Transformer<CDateRange, Integer>() {
						@Override
						public Integer transform(CDateRange value) {
							return PackedUnsigned1616.pack(value.getMinValue() - minValue, value.getMaxValue() - minValue);
						}
					},
					type
			);
		}
		
		return new Decision<>(
			new NoopTransformer<>(),
			new DateRangeTypeDateRange()
		);
	}
}
