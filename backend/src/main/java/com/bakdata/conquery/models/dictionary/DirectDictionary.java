package com.bakdata.conquery.models.dictionary;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.models.types.specific.VarIntType;

import jersey.repackaged.com.google.common.collect.Iterators;

public class DirectDictionary extends AStringType<Integer> {

	private final Dictionary dict;
	
	public DirectDictionary(Dictionary dict) {
		super(int.class);
		this.dict = dict;
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
	public boolean canStoreNull() {
		return false;
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
	public void adaptUnderlyingDictionary(Dictionary newDict, VarIntType newNumberType) {
		throw new UnsupportedOperationException();
	}
}
