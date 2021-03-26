package com.bakdata.conquery.models.dictionary;

import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Dataset;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString(of = "original")
@CPSType(id = "SHARED", base = Dictionary.class)
public class SharedDictionary extends Dictionary{

	@NsIdRef
	private final Dictionary original;

	public SharedDictionary(Dataset dataset, String name, Dictionary original) {
		super(name);
		this.original = original;
	}

	@Override
	public int add(byte[] bytes) {
		return original.add(bytes);
	}

	@Override
	public int put(byte[] bytes) {
		return original.put(bytes);
	}

	@Override
	public int getId(byte[] bytes) {
		return original.getId(bytes);
	}

	@Override
	public byte[] getElement(int id) {
		return original.getElement(id);
	}

	@Override
	public int size() {
		return original.size();
	}

	@Override
	public long estimateMemoryConsumption() {
		return original.estimateMemoryConsumption();
	}

	@NotNull
	@Override
	public Iterator<DictionaryEntry> iterator() {
		return original.iterator();
	}
}
