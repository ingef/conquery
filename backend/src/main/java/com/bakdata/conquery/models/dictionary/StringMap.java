package com.bakdata.conquery.models.dictionary;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface StringMap extends Iterable<String> {

	StringMap uncompress();

	int add(byte[] bytes);

	int get(byte[] bytes);

	String getElement(int id);

	byte[] getElementBytes(int id);

	int size();

	void compress();

	List<String> getValues();

	default void checkCompressed(String errorMessage) {
		if (!isCompressed()) {
			throw new IllegalStateException(errorMessage);
		}
	}

	boolean isCompressed();

	default void checkUncompressed(String errorMessage) {
		if (isCompressed()) {
			throw new IllegalStateException(errorMessage);
		}
	}

	int add(String element);
}
