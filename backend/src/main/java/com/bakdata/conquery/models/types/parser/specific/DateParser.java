package com.bakdata.conquery.models.types.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.DateTypeVarInt;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.util.DateFormats;
import lombok.ToString;

@ToString(callSuper = true)
public class DateParser extends Parser<Integer> {

	private VarIntParser subType = new VarIntParser(); 
	
	@Override
	protected Integer parseValue(@Nonnull String value) throws ParsingException {
		return CDate.ofLocalDate(DateFormats.parseToLocalDate(value));
	}
	
	@Override
	public Integer addLine(Integer v) {
		super.addLine(v);
		return subType.addLine(v);
	}

	@Override
	protected Decision<Integer, ?, DateTypeVarInt> decideType() {
		Decision<Integer, Number, VarIntType> subDecision = subType.findBestType();
		return new Decision<Integer, Number, DateTypeVarInt>(
			subDecision.getTransformer(),
			new DateTypeVarInt(subDecision.getType())
		);
	}
}
