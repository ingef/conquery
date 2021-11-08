package com.bakdata.conquery.models.preproc.outputs;

import java.util.Arrays;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.CompoundDateRangeParser;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.ToString;

/**
 * Parse input columns as {@link CDateRange}. Input values must be {@link com.bakdata.conquery.models.common.CDate} based ints.
 */
@Data
@ToString(of = {"startColumn", "endColumn"})
@CPSType(id = "COMPOUND_DATE_RANGE", base = OutputDescription.class)
public class CompoundDateRangeOutput extends OutputDescription {

	@NotNull
	@NotEmpty
	private String startColumn, endColumn;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers, DateReader dateReader) {
		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser type, long sourceLine) throws ParsingException {
				return null;
			}
		};
	}

	/**
	 * This function checks if the end-column really exists.
	 */
	@JsonIgnore
	@ValidationMethod(message = "End-column not found")
	public boolean isEndColumnPresent() {
		return Arrays.stream(getParent().getOutput())
					 .filter(output -> output.getName().equals(getEndColumn()))
					 .anyMatch(output -> output.getResultType().equals(MajorTypeId.DATE));
	}

	/**
	 * This function checks if the start-column really exists.
	 */
	@JsonIgnore
	@ValidationMethod(message = "Start-column not found")
	public boolean isStartColumnPresent() {
		return Arrays.stream(getParent().getOutput())
					 .filter(output -> output.getName().equals(getStartColumn()))
					 .anyMatch(output -> output.getResultType().equals(MajorTypeId.DATE));
	}

	/**
	 * The resulting type after {@link Output} has been applied.
	 */
	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE_RANGE;
	}

	public Parser<?, ?> createParser(ConqueryConfig config) {
		return new CompoundDateRangeParser(config, startColumn, endColumn);
	}
}
