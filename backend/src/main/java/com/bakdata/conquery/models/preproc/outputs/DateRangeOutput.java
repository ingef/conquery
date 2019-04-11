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
import lombok.extern.slf4j.Slf4j;

@Slf4j @Getter @Setter @CPSType(id="DATE_RANGE", base=Output.class)
public class DateRangeOutput extends Output {
	
	private static final long serialVersionUID = 1L;
	
	@Min(0)
	private int startColumn = -1;
	@Min(0)
	private int endColumn = -1;
	
	@Override
	public List<Object> createOutput(CType type, String[] row, int source, long sourceLine) throws ParsingException {
		if(row[startColumn]==null) {
			if(row[endColumn]==null) {
				return NULL;
			}
			else {
				LocalDate end = DateFormats.instance().parseToLocalDate(row[endColumn]);
				
				throw new ParsingException("No start date at "+startColumn+" while there is an end date at "+endColumn);
			}
		}
		else {
			LocalDate start = DateFormats.instance().parseToLocalDate(row[startColumn]);
			if(row[endColumn]==null) {
				throw new ParsingException("No end date at "+endColumn+" while there is a start date at "+startColumn);
			}
			else {
				LocalDate end = DateFormats.instance().parseToLocalDate(row[endColumn]);
				
				if(end.isBefore(start)) {
					throw new ParsingException("date range start "+start+" is after end "+end);
				}
				else {
					return Collections.singletonList(CDateRange.of(start, end));
				}
			}
		}
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}
}