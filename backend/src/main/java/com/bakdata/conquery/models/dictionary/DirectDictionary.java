package com.bakdata.conquery.models.dictionary;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import com.bakdata.conquery.models.types.specific.string.StringType;
import com.google.common.collect.Iterators;

public class DirectDictionary extends StringType {

	private final Dictionary dict;
	
	public DirectDictionary(Dictionary dict) {
		super();
		this.dict = dict;
	}


	// TODO why is this a StringType at all?

	@Override
	public StringType select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, Integer value) {

	}

	@Override
	public String getElement(int id) {
		return new String(dict.getElement(id), StandardCharsets.UTF_8);
	}

	@Override
	public int size() {
		return dict.size();
	}

	@Override
	public int getId(String value) {
		return dict.getId(value.getBytes(StandardCharsets.UTF_8));
	}

	public int add(String value) {
		return dict.add(value.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public Iterator<String> iterator() {
		return Iterators.transform(dict.iterator(), v->new String(v.getValue(), StandardCharsets.UTF_8));
	}

	public int put(String value) {
		return dict.put(value.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Integer.SIZE;
	}
	
	@Override
	public Dictionary getUnderlyingDictionary() {
		return dict;
	}
	
	@Override
	public void setUnderlyingDictionary(Dictionary newDict) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer get(int event) {
		return null;
	}

	@Override
	public boolean has(int event) {
		return false;
	}
}
