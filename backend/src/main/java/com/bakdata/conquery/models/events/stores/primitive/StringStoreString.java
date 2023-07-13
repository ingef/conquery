package com.bakdata.conquery.models.events.stores.primitive;

import java.util.Arrays;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@CPSType(id = "STRINGS", base = ColumnStore.class)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class StringStoreString implements StringStore {

	@ToString.Exclude
	private final String[] values;

	public static StringStoreString create(int size) {
		return new StringStoreString(new String[size]);
	}

	@JsonCreator
	public static StringStoreString withInternedStrings(String[] values) {
		for (int index = 0; index < values.length; index++) {
			values[index] = values[index].intern();
		}

		return new StringStoreString(values);
	}

	@Override
	public boolean has(int event) {
		return values[event] != null;
	}

	@Override
	public Object createScriptValue(int event) {
		return getString(event);
	}

	@Override
	public String getString(int event) {
		return values[event];
	}

	@Override
	public long estimateEventBits() {
		return 0;
	}

	@Override
	public int getLines() {
		return values.length;
	}

	@Override
	public <T extends ColumnStore> T createDescription() {
		return null;
	}

	@Override
	public StringStoreString select(int[] starts, int[] lengths) {
		return new StringStoreString(ColumnStore.selectArray(starts, lengths, values, String[]::new));
	}

	@Override
	public void setNull(int event) {
		values[event] = null;
	}

	@Override
	public void setString(int event, String value) {
		values[event] = value;
	}

	@Override
	public int size() {
		return (int) Arrays.stream(values).distinct().count();
	}

	@Override
	public Stream<String> iterateValues() {
		return Arrays.stream(values).distinct();
	}
}
