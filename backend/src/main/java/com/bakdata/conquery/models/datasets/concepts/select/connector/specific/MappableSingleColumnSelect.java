package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.function.Function;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

public abstract class MappableSingleColumnSelect extends SingleColumnSelect {

	@Getter
	private final InternToExternMapper mapping;

	@JsonIgnore
	private final Function<Object, Object> mapper;

	public MappableSingleColumnSelect(@NsIdRef Column column,
									  @Nullable InternToExternMapper mapping) {
		super(column);
		this.mapping = mapping;

		if (mapping == null) {
			// No mapping -> return what is provided
			mapper = Function.identity();
		}
		else {
			mapper = this::applyMapping;
		}
	}


	@Override
	public void init() {
		if (mapping != null) {
			mapping.init();
		}
	}


	private String applyMapping(Object intern) {
		return intern == null ? "" : getMapping().external(intern.toString());
	}


	@Override
	public Object toExternalRepresentation(Object intern) {
		return mapper.apply(intern);
	}
}
