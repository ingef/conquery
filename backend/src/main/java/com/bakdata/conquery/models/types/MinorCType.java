package com.bakdata.conquery.models.types;

import javax.annotation.Nonnull;

public abstract class MinorCType<JAVA_TYPE, MAJOR_TYPE extends CType<?,MAJOR_TYPE>>  extends CType<JAVA_TYPE, MAJOR_TYPE>{
	public MinorCType(MajorTypeId typeId, Class<?> javaType) {
		super(typeId, javaType);
	}

	@Override
	final protected JAVA_TYPE parseValue(@Nonnull String value) {
		throw new IllegalStateException("Minor types should not be parsed.");
	}
	
	@Override
	public CType<?, MAJOR_TYPE> bestSubType() {
		throw new IllegalStateException("Minor types should not be subtyped.");
	}
}
