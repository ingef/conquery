package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Parse input column as {@link com.bakdata.conquery.models.common.CDate} based int.
 */
@Data
@EqualsAndHashCode(callSuper = true, of = {"inputColumn"})
@ToString(of = {"inputColumn"})
@CPSType(id = "EPOCH", base = OutputDescription.class)
public class EpochOutput extends OutputDescription {

	private static final long serialVersionUID = 1L;

	@NotNull
	private String inputColumn;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, inputColumn);
		final int columnIndex = headers.getInt(inputColumn);

		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser type, long sourceLine) throws ParsingException {
				if (row[columnIndex] == null) {
					return null;
				}


				return Integer.parseInt(row[columnIndex]);
			}
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE;
	}
}
