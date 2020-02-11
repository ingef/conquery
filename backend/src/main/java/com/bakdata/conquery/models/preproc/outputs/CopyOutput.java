package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Parse column as type.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@CPSType(id = "COPY", base = OutputDescription.class)
public class CopyOutput extends OutputDescription {

	private static final long serialVersionUID = 1L;

	@NotNull
	private final String inputColumn;

	@NotNull
	private final MajorTypeId inputType;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, inputColumn);

		final int column = headers.getInt(inputColumn);

		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser<?> type, long sourceLine) throws ParsingException {
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
