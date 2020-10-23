package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.FloatStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import lombok.Getter;

@CPSType(base=CType.class, id="REAL_FLOAT")
public class RealTypeFloat extends CType<Double, Double> {

	@Getter
	private final FloatStore delegate;

	public RealTypeFloat(FloatStore delegate) {
		super(MajorTypeId.REAL);
		this.delegate = delegate;
	}

	@Override
	public ColumnStore<Double> createStore(int size) {
		return FloatStore.create(size);
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Float.SIZE;
	}

	@Override
	public RealTypeFloat select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, Double value){
		getDelegate().set(event, value);
	}

	@Override
	public boolean has(int event){
		return getDelegate().has(event);
	}

	@Override
	public Double get(int event) {
		return getDelegate().get(event);
	}
}