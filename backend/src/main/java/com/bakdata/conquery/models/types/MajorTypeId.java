package com.bakdata.conquery.models.types;

import java.math.BigDecimal;
import java.util.function.Function;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.specific.BooleanParser;
import com.bakdata.conquery.models.types.parser.specific.DateParser;
import com.bakdata.conquery.models.types.parser.specific.DateRangeParser;
import com.bakdata.conquery.models.types.parser.specific.DecimalParser;
import com.bakdata.conquery.models.types.parser.specific.IntegerParser;
import com.bakdata.conquery.models.types.parser.specific.MoneyParser;
import com.bakdata.conquery.models.types.parser.specific.RealParser;
import com.bakdata.conquery.models.types.parser.specific.string.StringParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ClassUtils;

@RequiredArgsConstructor
public enum MajorTypeId implements MajorTypeIdHolder {

	STRING		(int.class, false, "String", StringParser::new),
	INTEGER		(long.class, false, "Integer", IntegerParser::new),
	BOOLEAN		(boolean.class, false, "Boolean", BooleanParser::new),
	REAL		(double.class, false, "Real", RealParser::new),
	DECIMAL		(BigDecimal.class, false, "Decimal", DecimalParser::new),
	MONEY		(long.class, false, "Money", MoneyParser::new),
	DATE		(int.class, true, "Date", DateParser::new),
	DATE_RANGE	(CDateRange.class, true, "DateRange", DateRangeParser::new);
	
	@Getter
	private final Class<?> primitiveType;
	@Getter
	private final boolean dateCompatible;
	@Getter
	private final String label;
	private final Function<ParserConfig, Parser<?>> supplier;
	
	public Parser<?> createParser(ParserConfig config) {
		return supplier.apply(config);
	}

	@Override
	public MajorTypeId getTypeId() {
		return this;
	}
	
	public Class<?> getBoxedType() {
		return ClassUtils.primitiveToWrapper(primitiveType);
	}
}
