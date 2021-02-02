package com.bakdata.conquery.models.events.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.util.DateFormats;
import lombok.ToString;

@ToString(callSuper = true)
public class DateParser extends Parser<Integer, IntegerDateStore> {

	private IntegerParser subType;

	public DateParser(ParserConfig config) {
		super(config);
		subType = new IntegerParser(config);
	}

	@Override
	public void setLines(int lines) {
		super.setLines(lines);
		subType.setLines(lines);
	}

	@Override
	protected Integer parseValue(@Nonnull String value) throws ParsingException {
		return CDate.ofLocalDate(DateFormats.parseToLocalDate(value));
	}

	@Override
	public Integer addLine(Integer v) {
		if(v == null){
			subType.addLine(null);
			return super.addLine(null);
		}

		super.addLine(v);

		return subType.addLine(v.longValue()).intValue();
	}

	@Override
	protected IntegerDateStore decideType() {
		IntegerStore subDecision = subType.findBestType();

		return new IntegerDateStore(subDecision);
	}
}
