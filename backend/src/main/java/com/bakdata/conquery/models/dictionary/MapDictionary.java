package com.bakdata.conquery.models.dictionary;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="MAP_DICTIONARY", base=StringMap.class)
@AllArgsConstructor
public class MapDictionary implements StringMap {

	private Object2IntOpenHashMap<String> value2Id;
	@Getter(onMethod_ = @JsonValue)
	private List<String> id2Value;
	
	public MapDictionary() {
		value2Id = new Object2IntOpenHashMap<>();
		value2Id.defaultReturnValue(-1);
		id2Value = new ArrayList<>();
	}
	
	@JsonCreator
	public MapDictionary(List<String> id2Value) {
		this.id2Value = id2Value;
		value2Id = new Object2IntOpenHashMap<>();
		value2Id.defaultReturnValue(-1);
		
		for(int i=0;i<id2Value.size();i++) {
			value2Id.put(id2Value.get(i), i);
		}
	}

	@Override
	public Iterator<String> iterator() {
		return id2Value.iterator();
	}

	@Override
	public StringMap uncompress() {
		return new MapDictionary(value2Id, id2Value);
	}

	@Override
	public int add(byte[] bytes) {
		return add(new String(bytes, StandardCharsets.UTF_8));
	}

	@Override
	public synchronized int add(String value) {
		checkUncompressed("Do not add values after compression");
		int id = value2Id.getInt(value);
		if(id == -1) {
			id = id2Value.size();
			value2Id.put(value, id);
			id2Value.add(value);
		}
		return id;
	}

	@Override
	public int get(byte[] bytes) {
		return get(new String(bytes, StandardCharsets.UTF_8));
	}

	private int get(String value) {
		return value2Id.getInt(value);
	}

	@Override
	public String getElement(int id) {
		return id2Value.get(id);
	}

	@Override
	public byte[] getElementBytes(int id) {
		return id2Value.get(id).getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public int size() {
		return id2Value.size();
	}

	@Override
	public void compress() {}

	@Override
	public List<String> getValues() {
		return id2Value;
	}

	@Override
	public boolean isCompressed() {
		return false;
	}
}
