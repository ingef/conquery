package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.IntegerParser;
import com.bakdata.conquery.util.DateReader;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Output a null value.
 */
@Data
@CPSType(id="NULL", base= OutputDescription.class)
public class NullOutput extends OutputDescription {
	
	private static final long serialVersionUID = 1L;

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
					   .append(super.hashCode())
					   .append(inputType.name())
					   .toHashCode();
	}

	@NotNull
	private MajorTypeId inputType;

	@Override
	public boolean isRequired() {
		return false;
	}

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers, DateReader dateReader) {
		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser type, long sourceLine) throws ParsingException {
				return null;
			}
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}

	public Parser<?, ?> createParser(ConqueryConfig config) {

		return inputType.createParser(config);
	}
}
