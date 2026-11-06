package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.google.common.collect.BiMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Getter
@RequiredArgsConstructor
@Slf4j
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
public class LabelMap implements Searchable {

	@ToString.Include
	private final FilterId id;
	@Delegate
	private final BiMap<String, String> delegate;

	private final int minSuffixLength;
	private final boolean generateSearchSuffixes;

	@Override
	public boolean isGenerateSuffixes() {
		return generateSearchSuffixes;
	}

	@Override
	public boolean isSearchDisabled() {
		return false;
	}


	@Override
	public String getSearchHandle() {
		return "label_map_" + id.toString();
	}
}
