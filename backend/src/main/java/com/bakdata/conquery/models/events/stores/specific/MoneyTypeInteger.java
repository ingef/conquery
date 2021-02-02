package com.bakdata.conquery.models.events.stores.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = ColumnStore.class, id = "MONEY_VARINT")
@Getter
@Setter
public class MoneyTypeInteger implements MoneyStore {

	protected IntegerStore numberType;

	@JsonCreator
	public MoneyTypeInteger(IntegerStore numberType) {
		this.numberType = numberType;
	}

	@Override
	public int getLines() {
		return numberType.getLines();
	}

	@Override
	public Long createScriptValue(Object value) {
		return (long) numberType.createScriptValue(value);
	}

	@Override
	public MoneyTypeInteger select(int[] starts, int[] length) {
		return new MoneyTypeInteger(numberType.select(starts, length));
	}

	@Override
	public long getMoney(int event) {
		return numberType.getInteger(event);
	}

	@Override
	public Long get(int event) {
		return getMoney(event);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[numberType=" + numberType + "]";
	}

	@Override
	public long estimateEventBits() {
		return numberType.estimateEventBits();
	}

	@Override
	public void set(int event, Object value) {
		numberType.set(event, value);
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}
}
