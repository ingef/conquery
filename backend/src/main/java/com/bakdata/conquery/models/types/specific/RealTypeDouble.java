package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.DoubleStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="REAL_DOUBLE")
public class RealTypeDouble extends CType<Double, Double> {

	private final DoubleStore delegate;

	public RealTypeDouble(DoubleStore delegate) {
		super(MajorTypeId.REAL);
		this.delegate = delegate;
	}

	@Override
	public ColumnStore createStore(int size) {
		return DoubleStore.create(size);
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Double.SIZE;
	}

	@Override
	public ColumnStore<Double> select(int[] starts, int[] length) {
		return new RealTypeDouble(delegate.select(starts,length));
	}

	@Override
	public void set(int event, Double value) {
		delegate.set(event,value);
	}

	@Override
	public Double get(int event) {
		return delegate.get(event);
	}

	@Override
	public boolean has(int event) {
		return delegate.has(event);
	}
}