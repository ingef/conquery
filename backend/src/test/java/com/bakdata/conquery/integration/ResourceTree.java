package com.bakdata.conquery.integration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import io.github.classgraph.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor @Getter @ToString
public class ResourceTree {
	@ToString.Include
	private final String name;
	@ToString.Exclude
	private final ResourceTree parent;
	@Setter
	private Resource value;

	@ToString.Exclude
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
		List<String> l = Stream
			.iterate(this, Objects::nonNull, ResourceTree::getParent)
			.map(ResourceTree::getName)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		Collections.reverse(l);
		return Joiner.on('/').join(l);
	}

	public ResourceTree reduce() {
		if (children.size() == 1) {
			return children.values().iterator().next().reduce();
		}
		return this;
	}
}
