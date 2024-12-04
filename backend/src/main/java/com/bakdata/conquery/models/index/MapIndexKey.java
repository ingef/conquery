package com.bakdata.conquery.models.index;

import java.net.URI;
import java.util.List;

import lombok.Data;

@Data
public class MapIndexKey implements IndexKey {

	private final URI csv;
	private final String internalColumn;
	private final String externalTemplate;
	private final boolean allowMultiple;



	@Override
	public List<String> getExternalTemplates() {
		return List.of(externalTemplate);
	}

	@Override
	public Index<String> createIndex(String defaultEmptyLabel) {
		if (allowMultiple){
			return new MapMultiIndex(externalTemplate);
		}
		return new MapIndex(externalTemplate);
	}

}
