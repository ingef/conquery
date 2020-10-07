package com.bakdata.conquery.models.types.parser;

import lombok.NonNull;

@FunctionalInterface
public interface Transformer<MAJOR_JAVA_TYPE, JAVA_TYPE> {

	public JAVA_TYPE transform(@NonNull MAJOR_JAVA_TYPE value);

}
