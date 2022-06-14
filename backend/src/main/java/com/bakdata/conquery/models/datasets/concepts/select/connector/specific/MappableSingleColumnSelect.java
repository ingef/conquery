package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.function.Function;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Null;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.SerdesTarget;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

public abstract class MappableSingleColumnSelect extends SingleColumnSelect {

	/**
	 * If a mapping was provided the mapping changes the aggregator result before it is processed by a {@link com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider}.
	 */
	@Getter
	@Valid
	@Nullable
	@NsIdRef(serdesTarget = SerdesTarget.MANAGER)
	private final InternToExternMapper mapping;

	@JsonIgnore
	protected final Function<Object, String> mapper;

	public MappableSingleColumnSelect(@NsIdRef Column column,
									  @Nullable InternToExternMapper mapping){
		super(column);
		this.mapping = mapping;

		if (mapping != null) {
			mapper = this::applyMapping;
		}
		else {
			mapper = null;
		}
	}


	@Override
	public void init() {
		if (mapping != null) {
			mapping.init();
		}
	}

	@Override
	public ResultType getResultType() {
		if (mapping == null) {
			return super.getResultType();
		}
		return new ResultType.StringT(mapper);

	}

	private String applyMapping(Object intern) {
		return intern == null ? "" : getMapping().external(intern.toString());
	}
}
