package com.bakdata.conquery.models.types;

import com.bakdata.conquery.models.types.specific.VarIntType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CTypeVarInt<T extends Number> extends CType<Long, T> {

	protected VarIntType numberType;

	public CTypeVarInt(MajorTypeId typeId, VarIntType numberType) {
		super(typeId);
		this.numberType = numberType;
	}

	@Override
	public abstract Object createScriptValue(T value);

	@Override
	public abstract Object createPrintValue(T value);

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[numberType=" + numberType + "]";
	}

	@Override
	public long estimateMemoryBitWidth() {
		return numberType.estimateMemoryBitWidth();
	}

	@Override
	public void set(int event, T value) {
		if (value == null) {
			numberType.set(event, null);
		}
		else {
			numberType.set(event, value.longValue());
		}
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}
}
