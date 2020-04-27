package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import com.google.common.base.Strings;
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

	/**
	 * Parse null values as open date-range if true.
	 */
	public boolean allowOpen = false;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, startColumn, endColumn);

		final int startIndex = headers.getInt(startColumn);
		final int endIndex = headers.getInt(endColumn);

		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser<?> type, long sourceLine) throws ParsingException {
				if (!allowOpen && (Strings.isNullOrEmpty(row[startIndex]) || Strings.isNullOrEmpty(row[endIndex]))) {
					throw new IllegalArgumentException("Open Ranges are not allowed.");
				}

				if (Strings.isNullOrEmpty(row[startIndex]) && Strings.isNullOrEmpty(row[endIndex])) {
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