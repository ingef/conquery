package com.bakdata.conquery.models.concepts.select.connector;

import java.util.EnumSet;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public abstract class SingleColumnSelect extends Select {

	@Getter
	@Setter
	@NsIdRef
	@NotNull
	@NonNull
	private Column column;

	/**
	 * Indicates if the values in the specified column belong to a categorical set
	 * (bounded number of values).
	 */
	@Getter
	@Setter
	private boolean categorical = false;

	@JsonIgnore
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.allOf(MajorTypeId.class);
	}

	@Override
	public ResultType getResultType() {
		if(categorical) {
			return ResultType.CategoricalT.INSTANCE;
		}
		
		return super.getResultType();
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
}
