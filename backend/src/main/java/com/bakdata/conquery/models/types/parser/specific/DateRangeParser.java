package com.bakdata.conquery.models.types.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.NoopTransformer;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.specific.DateRangeTypeDateRange;
import com.bakdata.conquery.models.types.specific.DateRangeTypePacked;
import com.bakdata.conquery.models.types.specific.DateRangeTypeQuarter;
import com.bakdata.conquery.util.PackedUnsigned1616;
import org.apache.commons.lang3.StringUtils;

public class DateRangeParser extends Parser<CDateRange> {

	private boolean onlyQuarters = true;
	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;
	
	@Override
	protected CDateRange parseValue(@Nonnull String value) throws ParsingException {
		return DateRangeParser.parseISORange(value);
	}
	
	@Override
	protected void registerValue(CDateRange v) {
		if(!v.isSingleQuarter()) {
			onlyQuarters = false;
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
		if(onlyQuarters) {
			DateRangeTypeQuarter type = new DateRangeTypeQuarter();
			return new Decision<>(
				new Transformer<CDateRange, Integer>() {
					@Override
					public Integer transform(CDateRange value) {
						return ((CDateRange)value).getMinValue();
					}
				},
				type
			);
		}
		if(maxValue - minValue <PackedUnsigned1616.MAX_VALUE) {
			DateRangeTypePacked type = new DateRangeTypePacked();
			type.setMinValue(minValue);
			type.setMaxValue(maxValue);
			return new Decision<>(
				new Transformer<CDateRange, Integer>() {
					@Override
					public Integer transform(CDateRange value) {
						CDateRange v = (CDateRange) value;
						if(v.getMaxValue()>Integer.MAX_VALUE || v.getMinValue()<Integer.MIN_VALUE) {
							throw new IllegalArgumentException(value+" is out of range");
						}
						return PackedUnsigned1616.pack(v.getMinValue()-minValue, v.getMaxValue()-minValue);
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
