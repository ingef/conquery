package com.bakdata.conquery.io.cps;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CPSMap implements Iterable<Entry<Class<?>, String>>{
	private Multimap<Class<?>, String> class2TypeLabels = HashMultimap.create();
	private Map<String, Class<?>> typeLabel2Class;

	public void add(String id, Class<?> type) {
		class2TypeLabels.put(type, id);
	}

	public void calculateInverse() {
		ImmutableMultimap<Class<?>, String> immutable = ImmutableMultimap.copyOf(class2TypeLabels);
		class2TypeLabels = immutable;
		
		ImmutableMultimap<String, Class<?>> rev = immutable.inverse();
		int failed = 0;
		for(Entry<String, Collection<Class<?>>> e:rev.asMap().entrySet()) {
			if(e.getValue().size()>1) {
				log.error("There are multiple objects with the type id {}: {}", e.getKey(), e.getValue());
				failed++;
			}
		}
		if(failed > 0) {
			throw new IllegalStateException(failed+" errors (see above)");
		}
		typeLabel2Class = rev.entries().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}
	
	public void merge(CPSMap other) {
		class2TypeLabels.putAll(other.class2TypeLabels);
	}

	@Override
	public Iterator<Entry<Class<?>, String>> iterator() {
		return class2TypeLabels.entries().iterator();
	}

	public Class<?> getClassFromId(String id) {
		return typeLabel2Class.get(id);
	}

	public Set<Class<?>> getClasses() {
		return class2TypeLabels.keySet();
	}

	public String getTypeIdForClass(Class<?> suggestedType) {
		Collection<String> ids = class2TypeLabels.get(suggestedType);
		if(ids.isEmpty()) {
			//check if other base
			CPSType anno = suggestedType.getAnnotation(CPSType.class);
			if(anno == null) {
				throw new IllegalStateException("There is no id for the class "+suggestedType+" for.");
			}
			else {
				return anno.id();
			}
		}
		else {
			return ids.iterator().next();
		}
	}

	public Collection<String> getTypeIds() {
		return class2TypeLabels.values();
	}

	@Getter
	private static final CPSMap EMPTY;
	static {
		EMPTY = new CPSMap();
		EMPTY.calculateInverse();
	}
}
