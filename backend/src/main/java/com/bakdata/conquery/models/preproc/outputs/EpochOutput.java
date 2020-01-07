package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;

/**
 * Parse input column as {@link com.bakdata.conquery.models.common.CDate} based int.
 */
@Data
@CPSType(id = "EPOCH", base = OutputDescription.class)
public class EpochOutput extends OutputDescription {

	private static final long serialVersionUID = 1L;

	@NotNull
	private String column;

	@JsonIgnore
	private int columnIndex;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, column);
		columnIndex = headers.getInt(column);

		return (row, type, sourceLine) -> {
			if (row[columnIndex] == null) {
				return null;
			}

			return Integer.parseInt(row[columnIndex]);
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE;
	}
}
