package com.bakdata.conquery.models.types;

import com.bakdata.conquery.models.types.specific.VarIntType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class CTypeVarInt extends CType<Long, Long> {

	protected VarIntType numberType;

	public CTypeVarInt(MajorTypeId typeId, VarIntType numberType) {
		super(typeId);
		this.numberType = numberType;
	}

	@Override
	public abstract Object createScriptValue(Long value);

	@Override
	public abstract Object createPrintValue(Long value);

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"[numberType=" + numberType + "]";
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return numberType.estimateMemoryBitWidth();
	}

	@Override
	public void set(int event, Long value) {
		numberType.set(event, value);
	}

	@Override
	public Long get(int event) {
		return numberType.get(event);
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}
}
