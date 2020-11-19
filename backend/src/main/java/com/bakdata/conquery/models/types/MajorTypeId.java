package com.bakdata.conquery.models.types;

import java.util.function.Function;

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

@RequiredArgsConstructor
public enum MajorTypeId implements MajorTypeIdHolder {

	STRING(false, "String", StringParser::new),
	INTEGER(false, "Integer", IntegerParser::new),
	BOOLEAN(false, "Boolean", BooleanParser::new),
	REAL(false, "Real", RealParser::new),
	DECIMAL(false, "Decimal", DecimalParser::new),
	MONEY(false, "Money", MoneyParser::new),
	DATE(true, "Date", DateParser::new),
	DATE_RANGE(true, "DateRange", DateRangeParser::new);

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
}
