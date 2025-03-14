package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.google.common.collect.BiMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Getter
@RequiredArgsConstructor
@Slf4j
@EqualsAndHashCode
public class LabelMap implements Searchable<FrontendValue> {

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
}
