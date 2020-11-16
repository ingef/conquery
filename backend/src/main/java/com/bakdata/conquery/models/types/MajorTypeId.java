package com.bakdata.conquery.models.types;

import java.math.BigDecimal;
import java.util.function.BiFunction;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.preproc.ColumnDescription;
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

	STRING(int.class, false, "String", StringParser::new),
	INTEGER(long.class, false, "Integer", (desc, conf) -> new IntegerParser(conf)),
	BOOLEAN(boolean.class, false, "Boolean", (desc, conf) -> new BooleanParser(conf)),
	REAL(double.class, false, "Real", (desc, conf) -> new RealParser(conf)),
	DECIMAL(BigDecimal.class, false, "Decimal", (desc, conf) -> new DecimalParser(conf)),
	MONEY(long.class, false, "Money", (desc, conf) -> new MoneyParser(conf)),
	DATE(int.class, true, "Date", (desc, conf) -> new DateParser(conf)),
	DATE_RANGE(CDateRange.class, true, "DateRange", (desc, conf) -> new DateRangeParser(conf));

	@Getter
	private final Class<?> primitiveType;
	@Getter
	private final boolean dateCompatible;
	@Getter
	private final String label;
	private final BiFunction<ColumnDescription, ParserConfig, Parser<?>> supplier;

	public Parser<?> createParser(ColumnDescription columnDescription, ParserConfig config) {
		return supplier.apply(columnDescription, config);
	}

	@Override
	public MajorTypeId getTypeId() {
		return this;
	}

	public Class<?> getBoxedType() {
		return ClassUtils.primitiveToWrapper(primitiveType);
	}
}
