package com.bakdata.conquery.models.preproc.parser.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.models.preproc.OutputRow;

class BigDecimalColumnValues extends ListColumnValues<BigDecimal> {

	@Override
	public int add(OutputRow outputRow, int outputIndex) {
		if (outputRow.isNull(outputIndex)) {
			return add(null);
		}

		return add(outputRow.getBigDecimal(outputIndex));
	}
}
