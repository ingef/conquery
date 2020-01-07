package com.bakdata.conquery.models.preproc.outputs;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.MajorTypeId;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;

/**
 * Outputs the current line in the file.
 */
@Data
@CPSType(id="LINE", base= OutputDescription.class)
public class LineOutput extends OutputDescription {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		return (row, type, sourceLine) -> (sourceLine);
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.INTEGER;
	}
}
