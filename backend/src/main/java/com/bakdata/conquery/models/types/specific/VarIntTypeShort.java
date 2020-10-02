package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ShortStore;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;

@CPSType(base=CType.class, id="VAR_INT_INT16")
@Getter
public class VarIntTypeShort extends VarIntType {

	private final short maxValue;
	private final short minValue;
	
	public VarIntTypeShort(short minValue, short maxValue) {
		super(short.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public ColumnStore createStore(ImportColumn column, Object[] objects) {
		// TODO: 03.09.2020 short store
		short[] values = new short[objects.length];

		for (int index = 0; index < objects.length; index++) {
			if (objects[index] == null) {
				values[index] = Short.MAX_VALUE;
			}

			values[index] = ((Number) objects[index]).shortValue();
		}

		return new ShortStore(column, values, Short.MAX_VALUE);
	}

	@Override
	public int toInt(Number value) {
		return value.shortValue();
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Short.SIZE;
	}
}
