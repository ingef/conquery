package com.bakdata.conquery.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DynamicContainer;

import io.github.classgraph.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor @Getter @ToString
public class ResourceTree {
	@ToString.Include
	private final String name;
	private final ResourceTree parent;
	@Setter
	private Resource value;
	private Map<String, ResourceTree> children = new HashMap<>();

	public void addAll(Iterable<Resource> resources) {
		for(Resource r : resources) {
			add(r);
		}
	}

	public void add(Resource r) {
		String[] parts = StringUtils.split(r.getPath(), '/');
		add(r, parts, 0);
	}

	private void add(Resource r, String[] parts, int index) {
		if(index == parts.length) {
			this.value = r;
			return;
		}
		children
			.computeIfAbsent(parts[index], name -> new ResourceTree(name, this))
			.add(r, parts, index+1);
	}

	public String getFullName() {
		return Stream
			.iterate(this, Objects::nonNull, ResourceTree::getParent)
			.filter(Objects::nonNull)
			.map(ResourceTree::getName)
			.collect(Collectors.joining("/"));
	}

	public ResourceTree reduce() {
		if(children.size() > 1) {
			return this;
		}
		else {
			return children.values().iterator().next().reduce();
		}
	}
}
