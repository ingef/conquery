package com.bakdata.conquery.models.preproc.parser.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.specific.MoneyIntStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.OutputRow;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;

@ToString(callSuper = true)
public class MoneyParser extends Parser<BigDecimal, MoneyStore> {

	private final int defaultFractionDigits;
	private BigDecimal maxValue = null;
	private BigDecimal minValue = null;

	public MoneyParser(ConqueryConfig config) {
		super(config);
		defaultFractionDigits = config.getPreprocessor().getParsers().getCurrency().getDefaultFractionDigits();
	}

	@Override
	protected BigDecimal parseValue(String value) throws ParsingException {
		return NumberParsing.parseMoney(value);
	}

	@Override
	public void parse(String value, OutputRow outputRow, int outputIndex) throws ParsingException {
		if (value == null) {
			outputRow.setNull(outputIndex);
			return;
		}

		try {
			outputRow.setBigDecimal(outputIndex, NumberParsing.parseMoney(value));
		}
		catch (Exception e) {
			throw parsingException(value, e);
		}
	}

	@Override
	public void addLine(OutputRow outputRow, int outputIndex) {
		if (outputRow.isNull(outputIndex)) {
			recordNullLine();
			return;
		}

		final BigDecimal value = outputRow.getBigDecimal(outputIndex);
		recordObjectLine(value);
		registerValue(value);
	}

	@Override
	protected void registerValue(BigDecimal v) {
		if (maxValue == null){
			maxValue = v;
		}
		if(minValue == null){
			minValue = v;
		}

		maxValue = maxValue.max(v);
		minValue = minValue.min(v);
	}

	@Override
	protected MoneyStore decideType() {
		return decideType(getLines());
	}

	@Override
	protected MoneyStore decideType(int storeLines) {
		IntegerParser subParser = new IntegerParser(getConfig());
		subParser.registerValue(maxValue.movePointRight(defaultFractionDigits).longValue());
		subParser.registerValue(minValue.movePointRight(defaultFractionDigits).longValue());
		subParser.setLines(getLines());
		subParser.setNullLines(getNullLines());
		IntegerStore subDecision = subParser.findBestType(storeLines);

		return new MoneyIntStore(subDecision, defaultFractionDigits);
	}

	@Override
	public void setValue(MoneyStore store, int event, BigDecimal value) {
		store.setMoney(event, value);
	}

	@Override
	public ColumnValues<BigDecimal> createColumnValues() {
		return new BigDecimalColumnValues();
	}

}
