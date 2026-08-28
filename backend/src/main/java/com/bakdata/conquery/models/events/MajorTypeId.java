package com.bakdata.conquery.models.events;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MajorTypeId {

	STRING(StringParser::new), INTEGER(IntegerParser::new), BOOLEAN(BooleanParser::new), REAL(RealParser::new), DECIMAL(
		DecimalParser::new), MONEY(MoneyParser::new), DATE(DateParser::new), DATE_RANGE(DateRangeParser::new);

	public static final Set<MajorTypeId> DATE_COMPATIBLE = EnumSet.of(DATE, DATE_RANGE);
	public static final Set<MajorTypeId> NUMERIC = EnumSet.of(INTEGER, REAL, DECIMAL, MONEY);

	private final Function<ConqueryConfig, Parser> supplier;

	public Parser createParser(ConqueryConfig config) {
		return supplier.apply(config);
	}

	public boolean isNumeric() {
		return NUMERIC.contains(this);
	}

	public boolean isDateCompatible() {
		return DATE_COMPATIBLE.contains(this);
	}
}
