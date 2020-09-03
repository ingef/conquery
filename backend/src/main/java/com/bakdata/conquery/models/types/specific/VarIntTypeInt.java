package com.bakdata.conquery.models.types.specific;

import java.util.Arrays;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.IntegerStore;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;

@CPSType(base=CType.class, id="VAR_INT_INT32")
@Getter
public class VarIntTypeInt extends VarIntType {

	private final int maxValue;
	private final int minValue;
	
	public VarIntTypeInt(int minValue, int maxValue) {
		super(int.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public ColumnStore createStore(ImportColumn column, Object[] objects) {
		// TODO: 03.09.2020 intStore
		return new IntegerStore(column, Arrays.stream(objects).mapToInt(Integer.class::cast).toArray(), Integer.MAX_VALUE);
	}

	@Override
	public int toInt(Number value) {
		return value.intValue();
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Integer.SIZE;
	}
}
