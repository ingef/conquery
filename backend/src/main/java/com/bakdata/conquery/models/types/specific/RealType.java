package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.ColumnStore;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(base= ColumnStore.class, id="REAL_FLOAT")
public class RealType extends ColumnStore<Double> {

	@Getter
	private final ColumnStore<Double> delegate;

	@JsonCreator
	public RealType(ColumnStore<Double> delegate) {
		super(MajorTypeId.REAL);
		this.delegate = delegate;
	}

	@Override
	public long estimateEventBytes() {
		return Float.SIZE;
	}

	@Override
	public RealType select(int[] starts, int[] length) {
		return new RealType(delegate.select(starts, length));
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