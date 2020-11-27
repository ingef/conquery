package com.bakdata.conquery.models.types.specific.string;

import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.specific.ChainedStringType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_SUFFIX")
public class StringTypeSuffix extends ChainedStringType {

	@NonNull
	private String suffix;

	@JsonCreator
	public StringTypeSuffix(StringType subType, String suffix) {
		super(subType);
		this.suffix = suffix;
	}

	@Override
	public StringType select(int[] starts, int[] length) {
		return new StringTypeSuffix(subType.select(starts, length), suffix);
	}

	@Override
	public String getElement(int value) {
		return subType.getElement(value) + suffix;
	}

	@Override
	public String createScriptValue(Integer value) {
		return subType.createScriptValue(value) + suffix;
	}

	@Override
	public int getId(String value) {
		if (value.endsWith(suffix)) {
			return subType.getId(value.substring(0, value.length() - suffix.length()));
		}
		return -1;
	}

	@Override
	public void setValueMapping(int[] mapping) {
		subType.setValueMapping(mapping);
	}

	@Override
	public Iterator<String> iterator() {
		Iterator<String> subIt = subType.iterator();
		return new Iterator<String>() {
			@Override
			public boolean hasNext() {
				return subIt.hasNext();
			}

			@Override
			public String next() {
				return subIt.next() + suffix;
			}
		};
	}

	@Override
	public String toString() {
		return "StringTypeSuffix[suffix=" + suffix + ", subType=" + subType + "]";
	}
}
