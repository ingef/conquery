package com.bakdata.conquery.models.preproc.outputs;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.IntegerParser;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonCreator;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Outputs the current line in the file.
 */
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@ToString
@CPSType(id="LINE", base= OutputDescription.class)
public class LineOutput extends OutputDescription {


	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers, DateReader dateReader, ConqueryConfig config) {
		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser type, long sourceLine) throws ParsingException {
				return sourceLine;
			}
		};
	}
	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.INTEGER;
	}
	@Override
	public Parser<?, ?> createParser(ConqueryConfig config) {

		return new IntegerParser(config);
	}
}
