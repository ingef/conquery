package com.bakdata.conquery.models.preproc.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.DateReader;
import lombok.SneakyThrows;
import lombok.ToString;

@ToString(callSuper = true)
public class DateParser extends Parser<Integer, DateStore> {

	private IntegerParser subType;
	private DateReader dateReader;

	public DateParser(ConqueryConfig config) {
		super(config);
		subType = new IntegerParser(config);
		dateReader = config.getLocale().getDateReader();

	}

	@Override
	public void setLines(int lines) {
		super.setLines(lines);
		subType.setLines(lines);
	}

	@Override
	protected Integer parseValue(@Nonnull String value) throws ParsingException {
		return CDate.ofLocalDate(dateReader.parseToLocalDate(value));
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
		IntegerStore subDecision = subType.findBestType();
		return new IntegerDateStore(subDecision);
	}

	@Override
	public void setValue(DateStore store, int event, Integer value) {
		store.setDate(event, value);
	}

	@SneakyThrows
	@Override
	public ColumnValues createColumnValues() {
		return new IntegerColumnValues();
	}

}
