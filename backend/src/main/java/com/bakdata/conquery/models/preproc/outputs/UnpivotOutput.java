package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "UNPIVOT", base = Output.class)
public class UnpivotOutput extends Output {

	private static final long serialVersionUID = 1L;

	@JsonIgnore
	private int[] columns;

	@NotNull
	private String[] inputColumns;

	@NotNull
	private MajorTypeId inputType;
	private boolean includeNulls = false;

	@Override
	public void setHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, inputColumns);

		columns = new int[inputColumns.length];

		for (int index = 0; index < inputColumns.length; index++) {
			columns[index] = headers.getInt(inputColumns[index]);
		}
	}

	@Override
	public List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) throws ParsingException {
		List<Object> parsedRows = new ArrayList<>(columns.length);

		for (int inputColumn : columns) {
			String value = row[inputColumn];
			if (value != null) {
				parsedRows.add(type.parse(value));
			}
			else if (includeNulls) {
				parsedRows.add(null);
			}
		}
		//to prevent an empty result if each column is null
		if (parsedRows.isEmpty()) {
			return NULL;
		}

		return parsedRows;
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}
}