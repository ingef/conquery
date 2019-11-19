package com.bakdata.conquery.model;

import com.bakdata.conquery.util.Doc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor @Getter @Accessors(fluent = true)
public class CreateableDoc implements Doc {
	
	private final String description;
	private final String example;

	@Override
	public Class<Doc> annotationType() {
		return Doc.class;
	}
}