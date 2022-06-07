package com.bakdata.conquery.models.preproc.outputs;

import java.util.Arrays;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.CompoundDateRangeParser;
import com.bakdata.conquery.models.preproc.parser.specific.DateParser;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.ToString;

/**
 * Output creating delegating store of start and end-Column neighbours.
 * <p>
 * This output will still parse and validate the data to ensure that some assertions are held (ie.: only open when allowOpen is set, and start <= end).
 */
@Data
@ToString(of = {"startColumn", "endColumn"})
@CPSType(id = "COMPOUND_DATE_RANGE", base = OutputDescription.class)
public class CompoundDateRangeOutput extends OutputDescription {

	@NotNull
	@NotEmpty
	private String startColumn, endColumn;
	private boolean allowOpen;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers, DateReader dateReader, ConqueryConfig config) {
		final Output startReader = Arrays.stream(getParent().getOutput())
										 .filter(output -> output.getName().equals(getStartColumn()))
										 .findFirst()
										 .orElseThrow()
										 .createForHeaders(headers, dateReader, config);

		final Output endReader = Arrays.stream(getParent().getOutput())
									   .filter(output -> output.getName().equals(getEndColumn()))
									   .findFirst()
									   .orElseThrow()
									   .createForHeaders(headers, dateReader, config);

		final DateParser dateParser = new DateParser(config);


		// This output only verifies that the parsed data is valid and present, it will not store the CDateRanges themselves
		// Obviously this mean doing the work twice, but it's still better than storing the data twice also.
		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser type, long sourceLine) throws ParsingException {

				final Object start = startReader.createOutput(row, dateParser, sourceLine);
				final Object end = endReader.createOutput(row, dateParser, sourceLine);

				if (start == null && end == null) {
					return false;
				}

				if (start == null || end == null) {
					if(!allowOpen) {
						throw new IllegalArgumentException("Open Ranges are not allowed.");
					}

					// Since it's not possible that BOTH are null either of them being null already implies an open and therefore valid range.
					return true;
				}

				if ((Integer) start > (Integer) end) {
					throw new ParsingException("Start is after End.");
				}

				return true;
			}
		};
	}

	public Parser<?, ?> createParser(ConqueryConfig config) {
		return new CompoundDateRangeParser(config, startColumn, endColumn);
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


}
