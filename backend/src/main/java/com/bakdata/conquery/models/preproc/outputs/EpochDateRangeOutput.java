package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.types.MajorTypeId;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;

/**
 * Parse input columns as {@link CDateRange}. Input values must be {@link com.bakdata.conquery.models.common.CDate} based ints.
 */
@Data
@CPSType(id = "EPOCH_DATE_RANGE", base = OutputDescription.class)
public class EpochDateRangeOutput extends OutputDescription {

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


			int start = Integer.parseInt(row[startIndex]);
			int end = Integer.parseInt(row[endIndex]);

			return CDateRange.of(start, end);
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}
}