package com.bakdata.conquery.models.datasets.concepts.filters;

import java.util.EnumSet;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
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
	private ColumnId column;

	@Override
	public List<ColumnId> getRequiredColumns() {
		return List.of(getColumn());
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns do not match required Type.")
	public boolean isValidColumnType() {
		final Column resolved = getColumn().resolve();
		final boolean acceptable = getAcceptedColumnTypes().contains(resolved.getType());

		if (!acceptable) {
			log.error("Column[{}] is of Type[{}]. Not one of [{}]", resolved.getId(), resolved.getType(), getAcceptedColumnTypes());
		}

		return acceptable;
	}

	@JsonIgnore
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.allOf(MajorTypeId.class);
	}
}
