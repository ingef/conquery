package com.bakdata.conquery.models.preproc.outputs;

import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.BooleanParser;
import com.bakdata.conquery.models.preproc.parser.specific.DateParser;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.models.preproc.parser.specific.DecimalParser;
import com.bakdata.conquery.models.preproc.parser.specific.IntegerParser;
import com.bakdata.conquery.models.preproc.parser.specific.MoneyParser;
import com.bakdata.conquery.models.preproc.parser.specific.RealParser;
import com.bakdata.conquery.models.preproc.parser.specific.StringParser;
import com.bakdata.conquery.util.DateReader;
import com.bakdata.conquery.util.NumberParsing;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Parse column as type.
 */
@Data
@NoArgsConstructor
@ToString(of = {"inputColumn", "inputType"})
@CPSType(id = "COPY", base = OutputDescription.class)
public class CopyOutput extends OutputDescription {

	public CopyOutput(String name, String inputColumn, MajorTypeId typeId) {
		setName(name);
		this.inputColumn = inputColumn;
		this.inputType = typeId;
	}


	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append(super.hashCode())
				.append(inputColumn)
				.append(inputType.name())
				.toHashCode();
	}

	@NotNull
	private String inputColumn;

	@NotNull
	private MajorTypeId inputType;

	@Override
	public Output<Parser<?, ?, ?>> createForHeaders(Object2IntArrayMap<String> headers, DateReader dateReader, ConqueryConfig config) {
		assertRequiredHeaders(headers, inputColumn);

		final int column = headers.getInt(inputColumn);
		final ValueWriter valueWriter = switch (inputType) {
			case STRING -> (value, parser) -> ((StringParser) parser).addString(value);
			case INTEGER -> (value, parser) -> ((IntegerParser) parser).addLong(NumberParsing.parseLong(value));
			case BOOLEAN -> (value, parser) -> ((BooleanParser) parser).addBoolean(BooleanParser.parseBoolean(value));
			case REAL -> (value, parser) -> ((RealParser) parser).addDouble(NumberParsing.parseDouble(value));
			case DECIMAL -> (value, parser) -> ((DecimalParser) parser).addDecimal(NumberParsing.parseBig(value));
			case MONEY -> (value, parser) -> ((MoneyParser) parser).addMoney(NumberParsing.parseMoney(value));
			case DATE -> (value, parser) -> ((DateParser) parser).addDate(CDate.ofLocalDate(dateReader.parseToLocalDate(value)));
			case DATE_RANGE -> (value, parser) -> ((DateRangeParser) parser).addDateRange(dateReader.parseToCDateRange(value));
		};

		return new Output<>() {
			@Override
			protected void processRow(String[] row, Parser<?, ?, ?> type, long sourceLine) throws ParsingException {
				final String value = row[column];
				if (value == null) {
					addNull(type, sourceLine);
					return;
				}

				try {
					valueWriter.write(value, type);
				}
				catch (Exception e) {
					throw new ParsingException("Failed to parse '%s' with %s".formatted(value, type.getClass().getSimpleName()), e);
				}
			}
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}

	public Parser<?, ?, ?> createParser(ConqueryConfig config) {
		return inputType.createParser(config);
	}

	@FunctionalInterface
	private interface ValueWriter {
		void write(String value, Parser<?, ?, ?> parser);
	}
}
