package com.bakdata.conquery.models.preproc.outputs;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.Min;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;

import lombok.Data;

@Data
@CPSType(id = "EPOCH", base = Output.class)
public class EpochOutput extends Output {

	private static final long serialVersionUID = 1L;

	@Min(0)
	private int column;

	@Override
	public List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) {
		if (row[column] == null) {
			return NULL;
		} else {
			return Collections.singletonList(
				Integer.parseInt(row[column])
			);
		}
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE;
	}
}
