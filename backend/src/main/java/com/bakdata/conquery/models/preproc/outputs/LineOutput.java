package com.bakdata.conquery.models.preproc.outputs;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter @CPSType(id="LINE", base=Output.class)
public class LineOutput extends Output {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public List<Object> createOutput(CType type, String[] row, int source, long sourceLine) {
		return Collections.singletonList(sourceLine);
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.INTEGER;
	}
}
