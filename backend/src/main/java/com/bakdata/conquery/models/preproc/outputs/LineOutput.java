package com.bakdata.conquery.models.preproc.outputs;

import java.util.Collections;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.MajorTypeId;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @CPSType(id="LINE", base= OutputDescription.class)
public class LineOutput extends OutputDescription {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		return (type, row, source, sourceLine) -> Collections.singletonList(sourceLine);
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.INTEGER;
	}
}
