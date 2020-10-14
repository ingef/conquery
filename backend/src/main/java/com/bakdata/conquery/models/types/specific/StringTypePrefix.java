package com.bakdata.conquery.models.types.specific;

import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.StringStore;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter @Setter
@CPSType(base = CType.class, id = "STRING_PREFIX")
public class StringTypePrefix extends AChainedStringType {

	@NonNull
	private String prefix;
	
	@JsonCreator
	public StringTypePrefix(AStringType<Number> subType, String prefix) {
		super(subType);
		this.prefix = prefix;
	}

	@Override
	public ColumnStore createStore(int size) {
		return StringStore.create(size, subType.getUnderlyingDictionary());
	}

	@Override
	public String getElement(int value) {
		return prefix+subType.getElement(value);
	}
	
	@Override
	public String createScriptValue(Number value) {
		return prefix+subType.createScriptValue(value);
	}
	
	@Override
	public int getId(String value) {
		if(value.startsWith(prefix)) {
			return subType.getId(value.substring(prefix.length()));
		}
		else {
			return -1;
		}
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
				return prefix+subIt.next();
			}
		};
	}
	
	@Override
	public String toString() {
		return "StringTypePrefix[prefix=" + prefix + ", subType=" + subType + "]";
	}
}
