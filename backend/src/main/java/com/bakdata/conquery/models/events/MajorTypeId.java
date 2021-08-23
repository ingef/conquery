package com.bakdata.conquery.models.events;

import java.util.function.Function;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.BooleanParser;
import com.bakdata.conquery.models.preproc.parser.specific.DateParser;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.models.preproc.parser.specific.DecimalParser;
import com.bakdata.conquery.models.preproc.parser.specific.IntegerParser;
import com.bakdata.conquery.models.preproc.parser.specific.MoneyParser;
import com.bakdata.conquery.models.preproc.parser.specific.RealParser;
import com.bakdata.conquery.models.preproc.parser.specific.StringParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

	public Parser createParser(ConqueryConfig config) {
		return supplier.apply(config);
	}
}
