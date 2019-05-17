package com.bakdata.conquery.models.types.specific;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.util.PackedUnsigned1616;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="DATE_RANGE") @Getter @Setter
public class DateRangeType extends CType<CDateRange, DateRangeType> {

	@JsonIgnore
	private transient boolean onlyQuarters = true;
	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;
	
	public DateRangeType() {
		super(MajorTypeId.DATE_RANGE, CDateRange.class);
	}

	@Override
	protected CDateRange parseValue(String value) throws ParsingException {
		return parseISORange(value);
	}

	public static CDateRange parseISORange(String value) throws ParsingException {
		if(value==null) {
			return null;
		}
		String[] parts = StringUtils.split(value, '/');
		if(parts.length!=2) {
			throw ParsingException.of(value, "daterange");
		}
		DateFormats formats = DateFormats.instance();


		return CDateRange.of(
				formats.parseToLocalDate(parts[0]),
				formats.parseToLocalDate(parts[1])
		);
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

	@Override
	public CType<?, DateRangeType> bestSubType() {
		if(onlyQuarters) {
			DateRangeTypeQuarter subType = new DateRangeTypeQuarter();
			subType.setLines(this.getLines());
			subType.setNullLines(this.getNullLines());
			return subType;
		}
		if(maxValue - minValue <PackedUnsigned1616.MAX_VALUE) {
			DateRangeTypePacked subType = new DateRangeTypePacked();
			subType.setLines(this.getLines());
			subType.setNullLines(this.getNullLines());
			subType.setMinValue(minValue);
			subType.setMaxValue(maxValue);
			return subType;
		}
		return this;
	}
	
	@Override
	public Object createPrintValue(CDateRange value) {
		if (value == null) {
			return "";
		}

		return value;
	}
	
	@Override
	public boolean canStoreNull() {
		return true;
	}
}