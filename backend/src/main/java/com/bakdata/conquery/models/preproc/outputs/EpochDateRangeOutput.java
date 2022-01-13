package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.util.DateReader;
import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Parse input columns as {@link CDateRange}. Input values must be {@link com.bakdata.conquery.models.common.CDate} based ints.
 */
@Data
@ToString(of = {"startColumn", "endColumn"})
@CPSType(id = "EPOCH_DATE_RANGE", base = OutputDescription.class)
public class EpochDateRangeOutput extends OutputDescription {

	@NotNull
	private String startColumn, endColumn;

	/**
	 * Parse null values as open date-range if true.
	 */
	private boolean allowOpen = false;

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
					   .append(super.hashCode())
					   .append(startColumn)
					   .append(endColumn)
					   .append(allowOpen)
					   .toHashCode();
	}

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers, DateReader dateReader) {
		assertRequiredHeaders(headers, startColumn, endColumn);

		final int startIndex = headers.getInt(startColumn);
		final int endIndex = headers.getInt(endColumn);

		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser type, long sourceLine) throws ParsingException {
				final boolean startNull = Strings.isNullOrEmpty(row[startIndex]);
				final boolean endNull = Strings.isNullOrEmpty(row[endIndex]);

				if (startNull && endNull) {
					return null;
				}

				if (!allowOpen && (startNull || endNull)) {
					throw new IllegalArgumentException("Open Ranges are not allowed.");
				}

				if(startNull){
					return CDateRange.atMost(Integer.parseInt(row[endIndex]));
				}

				if(endNull){
					return CDateRange.atLeast(Integer.parseInt(row[startIndex]));
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

	@Override
	public Parser<?, ?> createParser(ConqueryConfig config) {

		return new DateRangeParser(config);
	}
}