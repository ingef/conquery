package com.bakdata.conquery.models.index;

import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapIndex extends HashMap<String, String> implements Index<MapIndexKey,String>{

	@Override
	public String put(String key, String value) {
		if (containsKey(key)) {
			throw new IllegalArgumentException("The key '" + key + "' already exists in the index. Cannot map '" + key + "' -> '" + value + "'.");
		}
		return super.put(key, value);
	}
}
