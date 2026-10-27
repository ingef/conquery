package com.bakdata.conquery.models.preproc.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.DateReader;
import lombok.ToString;

@ToString(callSuper = true)
public class DateParser extends Parser<Integer, DateStore> {

	private IntegerParser subType;
	private DateReader dateReader;

	public DateParser(ConqueryConfig config) {
		super(config, new IntegerColumnValues());
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
	public int parseAndAdd(String value) throws ParsingException {
		if (value == null) {
			return add(null);
		}

		try {
			return addDate(CDate.ofLocalDate(dateReader.parseToLocalDate(value)));
		}
		catch (Exception e) {
			throw parsingException(value, e);
		}
	}

	@Override
	public int add(Integer value) {
		if (value == null) {
			recordNullLine();
			subType.registerNull();
			return requireColumnValues().add(null);
		}

		return addDate(value.intValue());
	}

	public int addDate(int value) {
		registerDate(value);
		return ((IntegerColumnValues) requireColumnValues()).addInt(value);
	}

	public int getDate(int event) {
		return ((IntegerColumnValues) requireColumnValues()).getInt(event);
	}

	void registerDate(int value) {
		recordLongLine(value);
		subType.registerLong(value);
	}

	@Override
	protected DateStore decideType() {
		return decideType(getLines());
	}

	@Override
	protected DateStore decideType(int storeLines) {
		IntegerStore subDecision = subType.findBestType(storeLines);
		return new IntegerDateStore(subDecision);
	}

	@Override
	public void setValue(DateStore store, int event, Integer value) {
		store.setDate(event, value);
	}

}
