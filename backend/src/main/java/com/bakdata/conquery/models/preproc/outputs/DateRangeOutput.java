package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@CPSType(id = "DATE_RANGE", base = Output.class)
public class DateRangeOutput extends Output {

	private static final long serialVersionUID = 1L;

	@NotNull
	private String startColumn, endColumn;

	@JsonIgnore
	private int start, end;

	@Override
	public void setHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, startColumn, endColumn);

		start = headers.getInt(startColumn);
		end = headers.getInt(endColumn);
	}

	@Override
	public List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) throws ParsingException {

		if (row[start] == null && row[end] == null) {
			return NULL;
		}

		if (row[start] == null) {
			throw new ParsingException("No start date at `" + startColumn + "` while there is an end date at `" + endColumn + "`");
		}

		if (row[end] == null) {
			throw new ParsingException("No end date at `" + endColumn + "` while there is a start date at `" + startColumn + "`");
		}

		LocalDate begin = DateFormats.instance().parseToLocalDate(row[this.start]);

		LocalDate end = DateFormats.instance().parseToLocalDate(row[this.end]);

		if (end.isBefore(begin)) {
			throw new ParsingException(String.format("DateRange begin `%s` is after end `%s`", begin, end));
		}

		return Collections.singletonList(CDateRange.of(begin, end));
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}
}