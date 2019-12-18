package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @CPSType(id="NULL", base=Output.class)
public class NullOutput extends Output {
	
	private static final long serialVersionUID = 1L;

	@NotNull
	private MajorTypeId inputType;
	
	@Override
	public List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) {
		return NULL;
	}

	@Override
	public void setHeaders(Object2IntArrayMap<String> headers) {
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}
}
