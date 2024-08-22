package com.bakdata.conquery.models.events.stores.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import lombok.Data;
import lombok.ToString;

@CPSType(base = ColumnStore.class, id = "MONEY_VARINT")
@Data
@ToString(of = "numberType")
public class MoneyIntStore implements MoneyStore {

	private final IntegerStore numberType;
	private final int decimalShift; //TODO this might require preprocessing, consider injecting this.


	@Override
	public int getLines() {
		return numberType.getLines();
	}

	@Override
	public MoneyIntStore createDescription() {
		return new MoneyIntStore(numberType.createDescription(), getDecimalShift());
	}

	@Override
	public MoneyIntStore select(int[] starts, int[] length) {
		return new MoneyIntStore(numberType.select(starts, length), getDecimalShift());
	}

	@Override
	public BigDecimal getMoney(int event) {
		return BigDecimal.valueOf(numberType.getInteger(event)).movePointLeft(decimalShift);
	}

	@Override
	public long estimateEventBits() {
		return numberType.estimateEventBits();
	}

	@Override
	public void setMoney(int event, BigDecimal value) {
		numberType.setInteger(event, value.movePointRight(decimalShift).longValue());
	}

	@Override
	public void setNull(int event) {
		numberType.setNull(event);
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}

	public void setParent(Bucket bucket) {
		// not used
	}
}
