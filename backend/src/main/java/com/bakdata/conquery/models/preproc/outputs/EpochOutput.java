package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.DateParser;
import com.bakdata.conquery.util.DateReader;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Parse input column as {@link com.bakdata.conquery.models.common.CDate} based int.
 */
@Data
@ToString(of = {"inputColumn"})
@CPSType(id = "EPOCH", base = OutputDescription.class)
public class EpochOutput extends OutputDescription {

	@NotNull
	private String inputColumn;

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
					   .append(super.hashCode())
					   .append(inputColumn)
					   .toHashCode();
	}

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers, DateReader dateReader, ConqueryConfig config) {
		assertRequiredHeaders(headers, inputColumn);
		final int columnIndex = headers.getInt(inputColumn);

		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser type, long sourceLine) throws ParsingException {
				if (row[columnIndex] == null) {
					return null;
				}


				return Integer.parseInt(row[columnIndex]);
			}
		};
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE;
	}

	@Override
	public Parser<?, ?> createParser(ConqueryConfig config) {

		return new DateParser(config);
	}

}
