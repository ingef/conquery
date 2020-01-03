package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;

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

		return (type, row, source, sourceLine) -> {
			if (row[columnIndex] == null) {
				return NULL;
			}

			return Integer.parseInt(row[columnIndex]);
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE;
	}
}
