package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.ToString;

/**
 * Parse input columns as {@link CDateRange}. Input values must be {@link com.bakdata.conquery.models.common.CDate} based ints.
 */
@Data
@ToString(of = {"startColumn", "endColumn"})
@CPSType(id = "EPOCH_DATE_RANGE", base = OutputDescription.class)
public class EpochDateRangeOutput extends OutputDescription {

	private static final long serialVersionUID = 1L;

	@NotNull
	private String startColumn, endColumn;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, startColumn, endColumn);

		final int startIndex = headers.getInt(startColumn);
		final int endIndex = headers.getInt(endColumn);

		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser<?> type, long sourceLine) throws ParsingException {
				if (row[startIndex] == null && row[endIndex] == null) {
					return null;
				}


				int start = Integer.parseInt(row[startIndex]);
				int end = Integer.parseInt(row[endIndex]);

				return CDateRange.of(start, end);
			}
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}
}