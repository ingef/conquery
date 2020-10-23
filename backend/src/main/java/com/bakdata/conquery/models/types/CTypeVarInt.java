package com.bakdata.conquery.models.types;

import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.specific.VarIntType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class CTypeVarInt<MAJOR_JAVA_TYPE extends Number> extends CType<MAJOR_JAVA_TYPE, Number> {

	protected VarIntType numberType;

	public CTypeVarInt(MajorTypeId typeId, VarIntType numberType) {
		super(typeId);
		this.numberType = numberType;
	}

	@Override
	public abstract Object createScriptValue(Number value);

	@Override
	public abstract Object createPrintValue(Number value);

	@Override
	public ColumnStore createStore(int size) {
		return numberType.createStore(size);
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
