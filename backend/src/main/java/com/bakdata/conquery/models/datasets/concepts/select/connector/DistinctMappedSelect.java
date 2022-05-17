package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id = "DISTINCT_MAPPED", base = Select.class)
public class DistinctMappedSelect extends DistinctSelect implements MappedSelect {
	@Getter
	private final InternToExternMapper mapping;

	@JsonCreator
	public DistinctMappedSelect(@NsIdRef Column column, InternToExternMapper mapping) {
		super(column);
		this.mapping = mapping;
	}

	@Override
	public void init() {
		mapping.init();
	}

	@Override
	public Object transformValue(Object intern) {
		return doTransformValue(intern);
	}
}
