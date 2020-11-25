package com.bakdata.conquery.models.types.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.base.DateStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.util.DateFormats;
import lombok.ToString;

@ToString(callSuper = true)
public class DateParser extends Parser<Integer> {

	private IntegerParser subType = new IntegerParser();

	public DateParser(ParserConfig config) {

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
	protected DateStore decideType() {
		CType<Long> subDecision = subType.findBestType();
		return new DateStore(subDecision);
	}
}
