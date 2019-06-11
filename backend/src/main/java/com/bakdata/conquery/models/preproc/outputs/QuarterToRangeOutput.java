package com.bakdata.conquery.models.preproc.outputs;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.Min;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;

import lombok.Data;

@Data
@CPSType(id = "QUARTER_TO_RANGE", base = Output.class)
public class QuarterToRangeOutput extends Output {

	private static final long serialVersionUID = 1L;

	@Min(0)
	private int yearColumn;
	@Min(0)
	private int quarterColumn;

	@Override
	public List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) {
		if (row[yearColumn] == null || row[quarterColumn] == null) {
			return NULL;
		} else {
			return Collections.singletonList(
				QuarterUtils.fromQuarter(
					Integer.parseInt(row[yearColumn]),
					Integer.parseInt(row[quarterColumn])
				)
			);
		}
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}
}
