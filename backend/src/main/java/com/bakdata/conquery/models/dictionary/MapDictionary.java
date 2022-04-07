package com.bakdata.conquery.models.dictionary;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Dataset;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.math.DoubleMath;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

@CPSType(id = "MAP_DICTIONARY", base = Dictionary.class)
public class MapDictionary extends Dictionary {

	//TODO afaik we only use ByteArrayList for its equals/hashcode?
	//TODO make readonly after compress

	private Object2IntOpenHashMap<String> value2Id;
	private List<String> id2Value;

	public MapDictionary(Dataset dataset, @NotNull String name) {
		super(dataset, name);
		value2Id = new Object2IntOpenHashMap<>();
		value2Id.defaultReturnValue(-1);
		id2Value = new ArrayList<>();
	}

	@Override
	public Dictionary copyEmpty() {
		final Dictionary copy = new MapDictionary(getDataset(), getName());

		return copy;
	}

	@JsonCreator
	public MapDictionary(Dataset dataset, String name, String[] id2Value) {
		super(dataset, name);
		if (id2Value == null) {
			id2Value = new String[0];
		}
		this.id2Value = new ArrayList<>(id2Value.length);
		value2Id = new Object2IntOpenHashMap<>(id2Value.length);
		value2Id.defaultReturnValue(-1);

		for (int i = 0; i < id2Value.length; i++) {
			String value = id2Value[i].intern();
			this.id2Value.add(value);
			value2Id.put(value, i);
		}
	}

	@JsonProperty
	public String[] getId2Value() {
		return id2Value.toArray(String[]::new);
	}

	@Override
	public int add(String value) {
		if (getId(value) != -1) {
			throw new IllegalStateException("there already was an element " + value);
		}

		value = value.intern();

		int id = id2Value.size();
		value2Id.put(value, id);
		id2Value.add(value);
		return id;
	}

	@Override
	public int put(String value) {
		value = value.intern();

		int id = value2Id.getInt(value);

		if (id == -1) {
			id = id2Value.size();
			value2Id.put(value, id);
			id2Value.add(value);
		}
		return id;
	}

	@Override
	public int getId(String bytes) {
		return value2Id.getInt(bytes);
	}

	@Override
	public String getElement(int id) {
		return id2Value.get(id);
	}

	@Override
	public int size() {
		return id2Value.size();
	}

	@Override
	public Iterator<DictionaryEntry> iterator() {


		ListIterator<String> it = id2Value.listIterator();
		return new Iterator<>() {
			@Override
			public DictionaryEntry next() {
				return new DictionaryEntry(it.nextIndex(), it.next());
			}

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}
		};
	}


	public static long estimateMemoryConsumption(long entries, long totalBytes) {
		return DoubleMath.roundToLong(
				//size of two collections and string object overhead
				entries * (48f + 8f / Hash.DEFAULT_LOAD_FACTOR)
				//number of string bytes
				+ totalBytes,
				RoundingMode.CEILING
		);
	}

	@Override
	public long estimateMemoryConsumption() {
		return MapDictionary.estimateMemoryConsumption(
				id2Value.size(),
				id2Value.stream().mapToLong(String::length).sum()
		);
	}
}
