package com.bakdata.conquery.models.preproc.outputs;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.DateFormats;
import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Parse input columns as {@link CDateRange}. Input values must be {@link com.bakdata.conquery.models.common.CDate} based ints.
 */
@Data
@EqualsAndHashCode(callSuper = true, of = {"startColumn", "endColumn"})
@ToString(of = {"startColumn", "endColumn"})
@CPSType(id = "DATE_RANGE", base = OutputDescription.class)
public class DateRangeOutput extends OutputDescription {

	@NotNull
	private String startColumn, endColumn;

	/**
	 * Parse null values as open date-range if true.
	 */
	public boolean allowOpen = false;

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
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, startColumn, endColumn);

		final int startIndex = headers.getInt(startColumn);
		final int endIndex = headers.getInt(endColumn);

		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser type, long sourceLine) throws ParsingException {

				if (Strings.isNullOrEmpty(row[startIndex]) && Strings.isNullOrEmpty(row[endIndex])) {
					return null;
				}

				if (!allowOpen && (Strings.isNullOrEmpty(row[startIndex]) || Strings.isNullOrEmpty(row[endIndex]))) {
					throw new IllegalArgumentException("Open Ranges are not allowed.");
				}

				LocalDate start = DateFormats.parseToLocalDate(row[startIndex]);
				LocalDate end = DateFormats.parseToLocalDate(row[endIndex]);

				return CDateRange.of(start, end);
			}
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}
}