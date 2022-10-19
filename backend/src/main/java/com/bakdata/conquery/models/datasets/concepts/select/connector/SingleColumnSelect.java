package com.bakdata.conquery.models.datasets.concepts.select.connector;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

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
	public SelectResultInfo getResultInfo(CQConcept cqConcept) {

		if(categorical){
			return new SelectResultInfo(this, cqConcept, Set.of(new SemanticType.CategoricalT()));
		}

		return new SelectResultInfo(this, cqConcept);
	}

	@Override
	public ResultType getResultType() {
		return super.getResultType();
	}

	@Nullable
	@Override
	public List<Column> getRequiredColumns() {
		return List.of(getColumn());
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
