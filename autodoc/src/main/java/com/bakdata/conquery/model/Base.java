package com.bakdata.conquery.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class Base {
	private Class<?> baseClass;
	private String description;
}
