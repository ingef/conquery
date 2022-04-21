package com.bakdata.conquery.models.datasets.concepts.select.connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.index.InternToExternMapping;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id = "DISTINCT_MAPPED", base = Select.class)
public class DistinctMappedSelect extends DistinctSelect {
	private final InternToExternMapping mapping;

	@JsonCreator
	public DistinctMappedSelect(@NsIdRef Column column, InternToExternMapping mapping) {
		super(column);
		this.mapping = mapping;
	}

	@Override
	public Object transformValue(Object intern) {
		if (!(intern instanceof List)) {
			throw new IllegalStateException(String.format("Expected a List got %s (Type: %s)", intern, intern.getClass().getName()));
		}

		final List<String> externList = new ArrayList<>();

		for (Object elem : ((List<?>) intern)) {
			final String external;
			try {
				external = mapping.external(String.valueOf(elem));
			}
			catch (IOException e) {
				log.warn("Error resolving {} to external", intern, e);
				continue;
			}
			externList.add(external);
		}
		return externList;
	}
}
