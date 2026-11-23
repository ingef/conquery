package com.bakdata.conquery.models.preproc.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.OutputRow;
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
	public void parse(String value, OutputRow outputRow, int outputIndex) throws ParsingException {
		if (value == null) {
			outputRow.setNull(outputIndex);
			return;
		}

		try {
			outputRow.setLong(outputIndex, CDate.ofLocalDate(dateReader.parseToLocalDate(value)));
		}
		catch (Exception e) {
			throw parsingException(value, e);
		}
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
	public void addLine(OutputRow outputRow, int outputIndex) {
		if (outputRow.isNull(outputIndex)) {
			recordNullLine();
			subType.addNull();
			return;
		}

		final long value = outputRow.getLong(outputIndex);
		recordLongLine(value);
		subType.addLong(value);
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

	@SneakyThrows
	@Override
	public ColumnValues createColumnValues() {
		return new IntegerColumnValues();
	}

}
