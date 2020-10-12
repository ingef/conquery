package com.bakdata.conquery.models.types.specific;

import java.util.Collections;
import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.BooleanStore;
import com.bakdata.conquery.models.events.stores.SingletonStringStore;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonCreator;
import jersey.repackaged.com.google.common.collect.Iterators;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(base = CType.class, id = "STRING_SINGLETON")
public class StringTypeSingleton extends AStringType<Boolean> {

	private final String singleValue;

	@JsonCreator
	public StringTypeSingleton(String singleValue) {
		super(boolean.class);
		this.singleValue = singleValue;
	}

	@Override
	public ColumnStore createStore(int size) {
		return new SingletonStringStore(singleValue, BooleanStore.create(size));
	}


	@Override
	public int size() {
		return singleValue == null ?0:1;
	}

	@Override
	public String getElement(int id) {
		return singleValue;
	}
	
	@Override
	public String createScriptValue(Boolean value) {
		return singleValue;
	}

	@Override
	public boolean canStoreNull() {
		return true;
	}
	
	@Override
	public int getId(String value) {
		if(value != null && value.equals(singleValue)) {
			return 0;
		}
		else {
			return -1;
		}
	}
	
	@Override
	public Iterator<String> iterator() {
		if(singleValue == null) {
			return Collections.emptyIterator();
		}
		else {
			return Iterators.singletonIterator(singleValue);
		}
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Byte.SIZE;
	}
	
	@Override
	public Dictionary getUnderlyingDictionary() {
		return null;
	}
	
	@Override
	public void adaptUnderlyingDictionary(Dictionary newDict, VarIntType newNumberType) {
		throw new UnsupportedOperationException();
	}
}
