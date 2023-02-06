package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import it.unimi.dsi.fastutil.doubles.DoubleBigArrayBigList;
import it.unimi.dsi.fastutil.doubles.DoubleBigList;

class DoubleColumnValues extends ColumnValues<Double> {

	final DoubleBigList values = new DoubleBigArrayBigList();

	public DoubleColumnValues() {
		super(Double.NaN);
	}

	@Override
	public Double get(int event) {
		return values.getDouble(event);
	}

	@Override
	protected void append(Double obj) {
		values.add(obj.doubleValue());
	}

	@Override
	protected int size() {
		return (int) values.size64();
	}
}
