package com.bakdata.conquery.models.types.specific.string;

import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.specific.ChainedStringType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter @Setter
@CPSType(base = ColumnStore.class, id = "STRING_PREFIX")
public class StringTypePrefix extends ChainedStringType {

	@NonNull
	private String prefix;
	
	@JsonCreator
	public StringTypePrefix(StringType subType, String prefix) {
		super(subType);
		this.prefix = prefix;
	}

	@Override
	public String getElement(int value) {
		return prefix+subType.getElement(value);
	}
	
	@Override
	public String createScriptValue(Integer value) {
		return prefix+subType.createScriptValue(value);
	}
	
	@Override
	public int getId(String value) {
		if(value.startsWith(prefix)) {
			return subType.getId(value.substring(prefix.length()));
		}
		return -1;
	}

	@Override
	public void setIndexStore(CType<Long> indexStore) {
		subType.setIndexStore(indexStore);
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

	@Override
	public StringTypePrefix select(int[] starts, int[] length) {
		return new StringTypePrefix(subType.select(starts, length),getPrefix());
	}
}
