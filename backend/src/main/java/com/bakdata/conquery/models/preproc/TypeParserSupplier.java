package com.bakdata.conquery.models.preproc;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.*;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TypeParserSupplier {

	@SuppressWarnings("unchecked")
	public static <P extends Parser<?,?>> P createParser(MajorTypeId typeId, ConqueryConfig config) {
		return (P) switch (typeId) {
			case STRING -> new StringParser(config);
			case INTEGER -> new IntegerParser(config);
			case BOOLEAN -> new BooleanParser(config);
			case REAL -> new RealParser(config);
			case DECIMAL -> new DecimalParser(config);
			case MONEY -> new MoneyParser(config);
			case DATE -> new DateParser(config);
			case DATE_RANGE -> new DateRangeParser(config);
		};
	}
}
