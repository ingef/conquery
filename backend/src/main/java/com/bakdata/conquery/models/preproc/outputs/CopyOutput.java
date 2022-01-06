package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.DateReader;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Parse column as type.
 */
@Data
@NoArgsConstructor
@ToString(of = {"inputColumn", "inputType"})
@CPSType(id = "COPY", base = OutputDescription.class)
@Slf4j
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
	public Output createForHeaders(Object2IntArrayMap<String> headers, DateReader dateReader) {
		assertRequiredHeaders(headers, inputColumn);

		final int column = headers.getInt(inputColumn);

		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser type, long sourceLine) throws ParsingException {
				log.trace("Registering `{}` in line {} for Output[{}]", row[column], sourceLine, this.getDescription().getName());

				if (row[column] == null) {
					return null;
				}

				return type.parse(row[column]);
			}
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}

	public Parser<?, ?> createParser(ConqueryConfig config) {
		return inputType.createParser(config);
	}
}
