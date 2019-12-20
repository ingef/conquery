package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Collections;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.types.MajorTypeId;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@CPSType(id = "DATE_RANGE", base = OutputDescription.class)
public class DateRangeOutput extends OutputDescription {

	private static final long serialVersionUID = 1L;

	@NotNull
	private String startColumn, endColumn;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, startColumn, endColumn);

		int startIndex = headers.getInt(startColumn);
		int endIndex = headers.getInt(endColumn);

		return (type, row, source, sourceLine) -> {
			if (row[startIndex] == null && row[endIndex] == null) {
				return NULL;
			}

			if (row[startIndex] == null) {
				throw new ParsingException("No start date at `" + startColumn + "` while there is an end date at `" + endColumn + "`");
			}

			if (row[endIndex] == null) {
				throw new ParsingException("No end date at `" + endColumn + "` while there is a start date at `" + startColumn + "`");
			}

			LocalDate begin = DateFormats.instance().parseToLocalDate(row[startIndex]);

			LocalDate end = DateFormats.instance().parseToLocalDate(row[endIndex]);

			if (end.isBefore(begin)) {
				throw new ParsingException(String.format("DateRange begin `%s` is after end `%s`", begin, end));
			}

			return Collections.singletonList(CDateRange.of(begin, end));
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}
}