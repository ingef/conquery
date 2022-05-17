package com.bakdata.conquery.models.datasets.concepts.select.connector;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.LuceneInternToExternMapper;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

@CPSType(id = "RANDOM_MAPPED", base = Select.class)
public class RandomValueMappedSelect extends FirstValueSelect implements MappedSelect {

	@JsonIgnore
	@Getter
	private final InternToExternMapper mapping;

	@JsonCreator
	public RandomValueMappedSelect(@NsIdRef Column column, @NotNull InternToExternMapper mapping) {
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