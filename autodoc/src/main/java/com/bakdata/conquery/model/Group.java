package com.bakdata.conquery.model;

import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter @Builder
public class Group {

	private String name;
	private String description;
	@Singular
	private List<Class<?>> resources;
	@Singular
	private List<Base> bases;
	@Singular
	private Set<Class<?>> otherClasses;
	@Singular
	private List<Class<?>> hides;
	@Singular
	private List<Class<?>> markerInterfaces;
}