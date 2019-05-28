package com.bakdata.conquery.models.types.parser;

import lombok.Getter;
import lombok.NonNull;

@Getter 
public abstract class Transformer<MAJOR_JAVA_TYPE, JAVA_TYPE> {

	public abstract JAVA_TYPE transform(@NonNull MAJOR_JAVA_TYPE value);

	public void finishTransform() {}
}
