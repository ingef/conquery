package com.bakdata.conquery.models.events.stores.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = ColumnStore.class, id = "MONEY_VARINT")
@Getter
@Setter
public class MoneyTypeInteger extends ColumnStore<Long> {

	protected ColumnStore<Long> numberType;

	@JsonCreator
	public MoneyTypeInteger(ColumnStore<Long> numberType) {
		this.numberType = numberType;
	}

	@Override
	public Object createPrintValue(Long value) {
		return createScriptValue(value);
	}

	@Override
	public Long createScriptValue(Long value) {
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
	public void set(int event, Long value) {
		if (value == null) {
			numberType.set(event, null);
		}
		else {
			numberType.set(event, value.longValue());
		}
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}
}
