package com.bakdata.conquery.models.types.parser;

import lombok.Getter;
import lombok.NonNull;

@Getter 
public class NoopTransformer<JAVA_TYPE> implements Transformer<JAVA_TYPE, JAVA_TYPE> {

	@Override
	public JAVA_TYPE transform(@NonNull JAVA_TYPE value) {
		return value;
	}
}
