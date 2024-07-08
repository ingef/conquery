package com.bakdata.conquery.models.index;

import java.net.URI;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MapIndexKey extends AbstractIndexKey<MapIndex> {

	private final String externalTemplate;
	private final boolean allowMultiple;


	public MapIndexKey(URI csv, String internalColumn, String externalTemplate, boolean allowMultiple) {
		super(csv, internalColumn);
		this.externalTemplate = externalTemplate;
		this.allowMultiple = allowMultiple;
	}

	@Override
	public List<String> getExternalTemplates() {
		return List.of(externalTemplate);
	}

	@Override
	public MapIndex createIndex(String defaultEmptyLabel) {
		return new MapIndex(externalTemplate, allowMultiple);
	}

}
