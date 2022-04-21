package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.index.InternToExternMapping;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;

@CPSType(id = "LAST_MAPPED", base = Select.class)
public class LastValueMappedSelect extends FirstValueSelect implements SingleValueMappedSelect {

	@JsonIgnore
	private final InternToExternMapping mapping;

	public LastValueMappedSelect(@NsIdRef Column column, @JacksonInject InternToExternMapping mapping) {
		super(column);
		this.mapping = mapping;
	}

	@Override
	public InternToExternMapping getMapping() {
		return mapping;
	}

	@Override
	public Object transformValue(Object intern) {
		return doTransformValue(intern);
	}
}
