package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.MajorTypeId;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;

/**
 * Output a null value.
 */
@Data
@CPSType(id="NULL", base= OutputDescription.class)
public class NullOutput extends OutputDescription {
	
	private static final long serialVersionUID = 1L;

	@NotNull
	private MajorTypeId inputType;

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		return (row, type, sourceLine) -> null;
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}
}
