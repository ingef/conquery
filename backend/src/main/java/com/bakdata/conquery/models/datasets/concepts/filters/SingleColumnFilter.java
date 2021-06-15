package com.bakdata.conquery.models.datasets.concepts.filters;

import java.util.EnumSet;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SingleColumnFilter<FE_TYPE> extends Filter<FE_TYPE> {

	@Valid
	@NotNull
	@Getter
	@Setter
	@NsIdRef
	private Column column;

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getColumn()};
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns do not match required Type.")
	public boolean isValidColumnType() {
		final boolean acceptable = getAcceptedColumnTypes().contains(getColumn().getType());

		if (!acceptable) {
			log.error("Column[{}] is of Type[{}]. Not one of [{}]", column.getId(), column.getType(), getAcceptedColumnTypes());
		}

		return acceptable;
	}

	@JsonIgnore
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.allOf(MajorTypeId.class);
	}
}
