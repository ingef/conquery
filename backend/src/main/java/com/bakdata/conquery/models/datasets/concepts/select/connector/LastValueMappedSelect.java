package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;

@CPSType(id = "LAST_MAPPED", base = Select.class)
public class LastValueMappedSelect extends FirstValueSelect implements MappedSelect {

	@JsonIgnore
	@Getter
	private final InternToExternMapper mapping;

	@JsonCreator
	public LastValueMappedSelect(@NsIdRef Column column, @NonNull InternToExternMapper mapping) {
		super(column);
		this.mapping = mapping;
	}

	@Override
	public String transformValue(Object intern) {
		return doTransformValue(intern);
	}
}
