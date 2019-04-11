package com.bakdata.conquery.models.preproc.outputs;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter @CPSType(id="NULL", base=Output.class)
public class NullOutput extends Output {
	
	private static final long serialVersionUID = 1L;
	

	@NotNull
	private MajorTypeId inputType;
	
	@Override
	public List<Object> createOutput(CType<?,?> type, String[] row, int source, long sourceLine) {
		return NULL;
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}
}
