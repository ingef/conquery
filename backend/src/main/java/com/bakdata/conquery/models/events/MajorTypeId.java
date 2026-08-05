package com.bakdata.conquery.models.events;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor
public enum MajorTypeId {

	STRING(false, StringParser::new),
	INTEGER(false, IntegerParser::new),
	BOOLEAN(false, BooleanParser::new),
	REAL(false, RealParser::new),
	DECIMAL(false, DecimalParser::new),
	MONEY(false, MoneyParser::new),
	DATE(true, DateParser::new),
	DATE_RANGE(true, DateRangeParser::new);
	@Getter
	private final boolean dateCompatible;
	private final Function<ConqueryConfig, Parser> supplier;

	public static Set<MajorTypeId> numeric() {
		return Set.of(INTEGER, REAL, DECIMAL, MONEY);
	}

	public Parser createParser(ConqueryConfig config) {
		return supplier.apply(config);
	}
}
