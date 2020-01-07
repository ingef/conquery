package com.bakdata.conquery.models.preproc.outputs;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.util.DateFormats;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;


/**
 * Parse input columns as {@link CDateRange}. Columns must be string values.
 */
@Data
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

		return (row, type, sourceLine) -> {
			if (row[startIndex] == null && row[endIndex] == null) {
				return null;
			}

			LocalDate begin = DateFormats.parseToLocalDate(row[startIndex]);
			LocalDate end = DateFormats.parseToLocalDate(row[endIndex]);


			return CDateRange.of(begin, end);
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}
}