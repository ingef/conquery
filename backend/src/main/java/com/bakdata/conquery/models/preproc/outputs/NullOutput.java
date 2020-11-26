package com.bakdata.conquery.models.preproc.outputs;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
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
	public boolean isRequired() {
		return false;
	}

	@Override
	public List<String> getRequiredHeaders() {
		return Collections.emptyList();
	}

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser<?> type, long sourceLine) throws ParsingException {
				return null;
			}
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}
}
