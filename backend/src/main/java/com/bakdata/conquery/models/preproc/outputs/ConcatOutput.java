package com.bakdata.conquery.models.preproc.outputs;

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
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
@CPSType(id = "CONCAT", base = Output.class)
public class ConcatOutput extends Output {

	private static final long serialVersionUID = 1L;
	public static final String DELIMITER = "|";
	private static final String DOUBLE_DELIMITER = DELIMITER + DELIMITER;

	@NotEmpty
	private String[] inputColumns;

	@JsonIgnore
	private int[] columns;

	@Override
	public List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) throws ParsingException {
		StringBuilder result = new StringBuilder();

		for (int c : columns) {
			result.append(escape(row[c])).append(DELIMITER);
		}

		result.setLength(result.length() - 1);
		return Collections.singletonList(type.parse(result.toString()));
	}

	@Override
	public void setHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, inputColumns);

		columns = new int[inputColumns.length];

		for (int index = 0; index < inputColumns.length; index++) {
			columns[index] = headers.getInt(inputColumns[index]);
		}
	}

	private String escape(String v) {
		return StringUtils.replace(v, DELIMITER, DOUBLE_DELIMITER);
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.STRING;
	}
}