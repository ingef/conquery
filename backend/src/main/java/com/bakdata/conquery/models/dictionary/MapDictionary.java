package com.bakdata.conquery.models.dictionary;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
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
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.ToString;

@CPSType(id = "MAP_DICTIONARY", base = Dictionary.class)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class MapDictionary extends Dictionary {

	private Object2IntOpenHashMap<ByteArrayList> value2Id;
	private List<ByteArrayList> id2Value;

	public MapDictionary(Dataset dataset, @NotNull String name) {
		super(dataset, name);
		value2Id = new Object2IntOpenHashMap<>();
		value2Id.defaultReturnValue(-1);
		id2Value = new ArrayList<>();
	}

	@JsonCreator
	public MapDictionary(Dataset dataset, String name, byte[][] id2Value) {
		super(dataset, name);
		if (id2Value == null) {
			id2Value = new byte[0][];
		}
		this.id2Value = new ArrayList<>(id2Value.length);
		value2Id = new Object2IntOpenHashMap<>(id2Value.length);
		value2Id.defaultReturnValue(-1);

		for (int i = 0; i < id2Value.length; i++) {
			ByteArrayList v = new ByteArrayList(id2Value[i]);
			this.id2Value.add(v);
			value2Id.put(v, i);
		}
	}

	@JsonProperty
	public byte[][] getId2Value() {
		final int size = id2Value.size();
		final byte[][] result = new byte[size][];

		for (int i = 0; i < size; i++) {
			result[i] = id2Value.get(i).elements();
		}
		return result;
	}

	@Override
	public int add(byte[] bytes) {
		ByteArrayList value = new ByteArrayList(bytes);
		int id = value2Id.getInt(value);
		if (id == -1) {
			id = id2Value.size();
			value2Id.put(value, id);
			id2Value.add(value);
		}
		else {
			throw new IllegalStateException("there already was an element " + Arrays.toString(bytes));
		}
		return id;
	}

	@Override
	public int put(byte[] bytes) {
		ByteArrayList value = new ByteArrayList(bytes);
		int id = value2Id.getInt(value);
		if (id == -1) {
			id = id2Value.size();
			value2Id.put(value, id);
			id2Value.add(value);
		}
		return id;
	}

	@Override
	public int getId(byte[] bytes) {
		return value2Id.getInt(new ByteArrayList(bytes));
	}

	@Override
	public byte[] getElement(int id) {
		return id2Value.get(id).elements();
	}

	@Override
	public int size() {
		return id2Value.size();
	}

	@Override
	public Iterator<DictionaryEntry> iterator() {
		ListIterator<ByteArrayList> it = id2Value.listIterator();
		return new Iterator<DictionaryEntry>() {
			@Override
			public DictionaryEntry next() {
				return new DictionaryEntry(it.nextIndex(), it.next().elements());
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
				id2Value.stream().mapToLong(ByteArrayList::size).sum()
		);
	}
}
