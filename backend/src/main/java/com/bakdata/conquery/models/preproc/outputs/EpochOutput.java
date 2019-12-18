package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;

@Data
@CPSType(id = "EPOCH", base = Output.class)
public class EpochOutput extends Output {

	private static final long serialVersionUID = 1L;

	@NotNull
	private String column;

	@JsonIgnore
	private int columnIndex;

	@Override
	public void setHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, column);
		columnIndex = headers.getInt(column);
	}

	@Override
	public List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) {
		if (row[columnIndex] == null) {
			return NULL;
		}

		return Collections.singletonList(
			Integer.parseInt(row[columnIndex])
		);
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE;
	}
}
