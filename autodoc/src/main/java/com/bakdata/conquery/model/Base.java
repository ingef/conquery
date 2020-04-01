package com.bakdata.conquery.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString(of="baseClass")
@AllArgsConstructor @Getter
public class Base {
	private Class<?> baseClass;
	private String description;
}
