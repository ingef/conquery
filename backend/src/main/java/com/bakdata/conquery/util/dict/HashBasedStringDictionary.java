package com.bakdata.conquery.util.dict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class HashBasedStringDictionary implements Iterable<String> {

	private static final int NO_ENTRY = -1;



	private Object2IntMap<String> elementToId;
	private List<String> idToElement = new ArrayList<>();
	private int nextId = 0;

	public HashBasedStringDictionary() {
		elementToId = new Object2IntOpenHashMap<>();
		elementToId.defaultReturnValue(NO_ENTRY);
	}

	public HashBasedStringDictionary(Iterator<DictionaryEntry> entries) {
		this();
		while (entries.hasNext()) {
			DictionaryEntry entry = entries.next();
			int id = entry.getId();
			if (nextId != id) {
				throw new IllegalStateException();
			}
			String value = entry.getValue();
			elementToId.put(value, id);
			idToElement.add(value);
			nextId++;
		}
	}

	public int add(String element) {
		int c = elementToId.getInt(element);
		if(c==NO_ENTRY) {
			elementToId.put(element,nextId);
			idToElement.add(element);
			return nextId++;
		}
		else {
			return c;
		}
	}

	/**
	 * @return the id of element or -1 if element is not part of the dictionary
	 */
	public int getId(String element) {
		return elementToId.getInt(element);
	}

	public String getElement(int id) {
		return idToElement.get(id);
	}

	@Override
	public Iterator<String> iterator() {
		return Iterators.unmodifiableIterator(idToElement.iterator());
	}

	public int size() {
		return idToElement.size();
	}

	public ObjectSet<Entry<String>> getEntries() {
		return elementToId.object2IntEntrySet();
	}

	@Getter @RequiredArgsConstructor @Setter
	public static class DictionaryEntry {

		private final String value;
		private final int id;

	}

	public Collection<String> values() {
		return idToElement;
	}

}

