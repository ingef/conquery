package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import java.util.Collections;
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
@CPSType(id = "COPY", base = Output.class)
public class CopyOutput extends Output {

	private static final long serialVersionUID = 1L;

	@NotNull
	private String inputColumn;

	@JsonIgnore
	private int column;

	@NotNull
	private MajorTypeId inputType;

	@Override
	public List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) throws ParsingException {
		if (row[column] == null) {
			return NULL;
		}

		return Collections.singletonList(type.parse(row[column]));
	}

	@Override
	public void setHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers,inputColumn);

		column = headers.getInt(inputColumn);
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}
}
