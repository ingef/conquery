package com.bakdata.eva.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter @Builder
public class Group {

	private String name;
	@Singular
	private List<Base> bases;
	@Singular
	private List<Class<?>> classes;
}
