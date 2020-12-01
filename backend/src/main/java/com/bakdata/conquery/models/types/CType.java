package com.bakdata.conquery.models.types;

import java.math.RoundingMode;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.math.LongMath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter @RequiredArgsConstructor
public abstract class CType<JAVA_TYPE> extends ColumnStore<JAVA_TYPE> implements MajorTypeIdHolder {

	@JsonIgnore
	@NotNull
	private transient final MajorTypeId typeId;

	private int lines = 0;
	private int nullLines = 0;

	public Object createScriptValue(JAVA_TYPE value) {
		return value;
	}

	public Object createPrintValue(JAVA_TYPE value) { return value != null ? createScriptValue(value) : ""; }



	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}


	public long estimateMemoryConsumption() {
		long width = estimateMemoryFieldSize();

		return LongMath.divide(
			(lines-nullLines) * width + nullLines * Math.min(Long.SIZE, width),
			8, RoundingMode.CEILING
		); 
	}

	public abstract long estimateMemoryFieldSize();
	
	public long estimateTypeSize() {
		return 0;
	}

	public abstract CType<JAVA_TYPE> select(int[] starts, int[] lengths);

}
