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

	public int addDouble(double value) {
		final int event = prepareAdd(false);
		values.add(value);
		return event;
	}

	@Override
	protected void truncateValues(int size) {
		values.removeElements(size, values.size64());
	}

	@Override
	protected int size() {
		return (int) values.size64();
	}
}
