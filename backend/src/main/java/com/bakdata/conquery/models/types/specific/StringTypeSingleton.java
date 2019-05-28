package com.bakdata.conquery.models.types.specific;

import java.util.Collections;
import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;

import jersey.repackaged.com.google.common.collect.Iterators;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(base = CType.class, id = "STRING_VARINT")
public class StringTypeSingleton extends CType<Integer, Boolean> implements IStringType {

	private final String singleValue;

	@JsonCreator
	public StringTypeSingleton(String singleValue) {
		super(MajorTypeId.STRING, boolean.class);
		this.singleValue = singleValue;
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
}
