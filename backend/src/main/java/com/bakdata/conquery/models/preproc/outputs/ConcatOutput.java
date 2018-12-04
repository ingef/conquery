package com.bakdata.conquery.models.preproc.outputs;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter @CPSType(id="CONCAT", base=Output.class)
public class ConcatOutput extends Output {

	private static final long serialVersionUID = 1L;
	public static final String DELIMETER = "|";
	private static final String DOUBLE_DELIMETER = DELIMETER+DELIMETER;
	
	@NotEmpty
	private int[] inputColumns;
	
	@Override
	public List<Object> createOutput(CType<?,?> type, String[] row, int source, long sourceLine) throws ParsingException {
		StringBuilder result = new StringBuilder();
		for(int c:inputColumns) {
			result.append(escape(row[c])).append(DELIMETER);
		}
		result.setLength(result.length()-1);
		return Collections.singletonList(type.parse(result.toString()));
	}

	private String escape(String v) {
		return StringUtils.replace(v, DELIMETER, DOUBLE_DELIMETER);
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.STRING;
	}
}