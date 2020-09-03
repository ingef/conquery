package com.bakdata.conquery.models.types;

import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.specific.VarIntType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class CTypeVarInt<MAJOR_JAVA_TYPE> extends CType<MAJOR_JAVA_TYPE, Number> {

	protected VarIntType numberType;

	public CTypeVarInt(MajorTypeId typeId, VarIntType numberType) {
		super(typeId, numberType.getPrimitiveType());
		this.numberType = numberType;
	}

	@Override
	public final ColumnStore createStore(ImportColumn column, Object[] objects) {
		return numberType.createStore(column,objects);
	}

	@Override
	public boolean canStoreNull() {
		return numberType.canStoreNull();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"[numberType=" + numberType + "]";
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return numberType.estimateMemoryBitWidth();
	}
}
