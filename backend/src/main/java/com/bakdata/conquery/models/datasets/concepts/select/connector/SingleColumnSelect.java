package com.bakdata.conquery.models.datasets.concepts.select.connector;

import java.util.EnumSet;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
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
@Setter
@Getter
public abstract class SingleColumnSelect extends Select {

	@NsIdRef
	@NotNull
	@NonNull
	private Column column;

	/**
	 * Indicates if the values in the specified column belong to a categorical set
	 * (bounded number of values).
	 */
	private boolean categorical = false;

	/**
	 * Overwritten for classes that have specializations.
	 */
	@JsonIgnore
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.allOf(MajorTypeId.class);
	}

	@Override
	public ResultType getResultType() {
		if (categorical) {
			return ResultType.CategoricalT.INSTANCE;
		}

		return super.getResultType();
	}

	@JsonIgnore
	@ValidationMethod(message = "Column does not match required Type.")
	public boolean isValidColumnType() {

		if (getAcceptedColumnTypes().contains(this.getColumn().getType())) {
			return true;
		}

		log.error("Column[{}] is of Type[{}]. Not one of [{}]", column.getId(), column.getType(), getAcceptedColumnTypes());

		return false;
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns is not for Connectors' Table.")
	public boolean isForConnectorTable() {

		if (getColumn().getTable().equals(((Connector) getHolder()).getTable())) {
			return true;
		}

		log.error("Column[{}] ist not for Table[{}]", column.getId(), ((Connector) getHolder()).getTable());

		return false;
	}
}
