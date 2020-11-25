package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.DoubleStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(base=ColumnStore.class, id="REAL_DOUBLE")
@Getter
public class RealTypeDouble extends CType<Double> {

	private final DoubleStore store;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public RealTypeDouble(DoubleStore delegate) {
		super(MajorTypeId.REAL);
		this.store = delegate;
	}

	@Override
	public long estimateMemoryFieldSize() {
		return Double.SIZE;
	}

	@Override
	public RealTypeDouble select(int[] starts, int[] length) {
		return new RealTypeDouble(store.select(starts, length));
	}

	@Override
	public void set(int event, Double value) {
		store.set(event, value);
	}

	@Override
	public Double get(int event) {
		return store.get(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}