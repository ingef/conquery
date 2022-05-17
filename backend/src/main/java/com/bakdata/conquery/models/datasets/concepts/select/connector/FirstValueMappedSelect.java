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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;

@CPSType(id = "FIRST_MAPPED", base = Select.class)
public class FirstValueMappedSelect extends FirstValueSelect implements MappedSelect {

	@Getter
	@NotNull
	private final InternToExternMapper mapping;

	@JsonCreator
	public FirstValueMappedSelect(@NsIdRef Column column,
								  @NotNull @NonNull InternToExternMapper mapping) {
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
