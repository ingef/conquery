package com.bakdata.conquery.models.index;

import java.net.URL;

public class MapIndexKey extends AbstractIndexKey<MapIndex, String>{

	public MapIndexKey(URL csv, String internalColumn, String externalTemplate) {
		super(csv, internalColumn, externalTemplate);
	}

	@Override
	public MapIndex createIndex() {
		return new MapIndex();
	}
}
