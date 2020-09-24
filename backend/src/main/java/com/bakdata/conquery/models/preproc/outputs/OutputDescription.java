package com.bakdata.conquery.models.preproc.outputs;

import java.io.Serializable;
import java.util.InputMismatchException;
import java.util.Objects;
import java.util.StringJoiner;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.ColumnDescription;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import javax.validation.constraints.NotEmpty;

/**
 * Output's are used for preprocessing to generate cqpp files. Their main function is to selectivley read data from an Input CSV and prepare it for fast reading into a live Conquery instance. An output describes the transformation of an input row into an output row. It can do some transformation but should avoid complex work.
 *
 * @apiNote we are currently aiming to reduce the functionality of the preprocessing step to provide basically only a mapping from input to table fields.
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "operation")
@CPSBase
public abstract class OutputDescription implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotEmpty
	private String name;

	private boolean required = false;

	/**
	 * Describes a transformation of an input row to a single value.
	 */
	public abstract class Output {

		public OutputDescription getDescription(){
			return OutputDescription.this;
		}

		/**
		 * Parse the line/row
		 * @param row the row to parse
		 * @param type the Parser for the emitted column
		 * @param sourceLine the linenumber of the row in the input file
		 * @return a value or null
		 * @throws ParsingException
		 */
		protected abstract Object parseLine(String[] row, Parser<?> type, long sourceLine) throws ParsingException;

		/**
		 * Parse row and test for NULL values, throwing an exception when Required but missing.
		 */
		public Object createOutput(String[] row, Parser<?> type, long sourceLine) throws ParsingException {
			if(OutputDescription.this.isRequired()) {
				return Objects.requireNonNull(
						parseLine(row, type, sourceLine),
						() -> String.format("Required Output[%s] produced NULL value at line %d=%s", OutputDescription.this.getName(), sourceLine, row)
				);
			}

			return parseLine(row, type, sourceLine);
		}
	}

	@Data
	public static class OutputException extends Exception {
		private final OutputDescription source;
		public OutputException(OutputDescription source, Exception cause){
			super(cause);
			this.source = source;
		}
	}

	/**
	 * Helper function to verify that all headers are present.
	 * Throws an exception of one is missing.
	 */
	protected void assertRequiredHeaders(Object2IntArrayMap<String> actualHeaders, String... headers) {
		StringJoiner missing = new StringJoiner(", ");

		for (String h : headers) {
			if (!actualHeaders.containsKey(h)) {
				missing.add(h);
			}
		}

		if (missing.length() != 0) {
			throw new InputMismatchException(String.format("Did not find headers `%s` in `%s`", missing.toString(), actualHeaders.keySet()));
		}
	}

	/**
	 * Instantiate the corresponding {@link Output} for the rows.
	 * @param headers A map from column names to column indices.
	 * @return the output for the specific headers.
	 */
	public abstract Output createForHeaders(Object2IntArrayMap<String> headers);

	/**
	 * The resulting type after {@link Output} has been applied.
	 */
	@JsonIgnore
	public abstract MajorTypeId getResultType();

	/**
	 * Create a new description for the column.
	 */
	@JsonIgnore
	public ColumnDescription getColumnDescription() {
		return new ColumnDescription(name, getResultType());
	}
}
