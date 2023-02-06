package com.bakdata.conquery.models.events.stores.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@CPSType(base = ColumnStore.class, id = "MONEY_VARINT")
@Getter
@Setter
@ToString(of = "numberType")
public class MoneyIntStore implements MoneyStore {

	protected IntegerStore numberType;

	@JsonCreator
	public MoneyIntStore(IntegerStore numberType) {
		this.numberType = numberType;
	}

	@Override
	public int getLines() {
		return numberType.getLines();
	}

	@Override
	public MoneyIntStore createDescription() {
		return new MoneyIntStore(numberType.createDescription());
	}

	@Override
	public MoneyIntStore select(int[] starts, int[] length) {
		return new MoneyIntStore(numberType.select(starts, length));
	}

	@Override
	public long getMoney(int event) {
		return numberType.getInteger(event);
	}

	@Override
	public long estimateEventBits() {
		return numberType.estimateEventBits();
	}

	@Override
	public void setMoney(int event, long value) {
		numberType.setInteger(event, value);
	}

	@Override
	public void setNull(int event) {
		numberType.setNull(event);
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}
}
