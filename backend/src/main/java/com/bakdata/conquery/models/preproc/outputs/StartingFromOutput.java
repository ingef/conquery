package com.bakdata.conquery.models.preproc.outputs;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.Min;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter @CPSType(id="STARTING_FROM", base=Output.class)
public class StartingFromOutput extends Output {
	
	private static final long serialVersionUID = 1L;
	
	@Min(0)
	private int inputColumn = -1;
	
	@Override
	public List<Object> createOutput(CType type, String[] row, int source, long sourceLine) throws ParsingException {
		if(row[inputColumn]==null) {
			return NULL;
		}
		else {
			LocalDate start = DateFormats.instance().parseToLocalDate(row[inputColumn]);
			return Collections.singletonList(CDateRange.atLeast(start));
		}
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}
}
