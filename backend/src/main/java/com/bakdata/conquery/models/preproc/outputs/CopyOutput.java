package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Parse column as type.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(of = {"inputColumn", "inputType"})
@CPSType(id = "COPY", base = OutputDescription.class)
@Slf4j
public class CopyOutput extends OutputDescription {

	public CopyOutput(String name, String inputColumn, MajorTypeId typeId){
		setName(name);
		this.inputColumn = inputColumn;
		this.inputType = typeId;
	}

	private static final long serialVersionUID = 1L;

	@NotNull
	private String inputColumn;

	@NotNull
	private MajorTypeId inputType;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
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
}
