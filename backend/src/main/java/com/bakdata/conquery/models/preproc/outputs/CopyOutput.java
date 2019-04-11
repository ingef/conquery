package com.bakdata.conquery.models.preproc.outputs;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter @CPSType(id="COPY", base=Output.class)
public class CopyOutput extends Output {
	
	private static final long serialVersionUID = 1L;
	

	@Min(0)
	private int inputColumn;
	@NotNull
	private MajorTypeId inputType;

	@Override
	public List<Object> createOutput(CType<?,?> type, String[] row, int source, long sourceLine) throws ParsingException {
		if(row[inputColumn]==null) {
			return NULL;
		}
		else {
			return Collections.singletonList(type.parse(row[inputColumn]));
		}
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}
}
