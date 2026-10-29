package com.bakdata.conquery.models.preproc.parser.specific;

import java.math.BigDecimal;
import java.util.Objects;

class BigDecimalColumnValues extends ListColumnValues<BigDecimal> {

	public void addBigDecimal(BigDecimal value) {
		Objects.requireNonNull(value, "value");

		prepareAdd(false);
		objects.add(value);
	}
}
