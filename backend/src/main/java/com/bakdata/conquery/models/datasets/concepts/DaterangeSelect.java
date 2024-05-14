package com.bakdata.conquery.models.datasets.concepts;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;

public interface DaterangeSelect {

	ColumnId getColumn();

	ColumnId getStartColumn();

	ColumnId getEndColumn();

	@JsonIgnore
	default Table getTable() {
		if (getColumn() != null) {
			return getColumn().resolve().getTable();
		}
		// start and end column are of the same table, so it does not matter which one we choose
		return getStartColumn().resolve().getTable();
	}

	@JsonIgnore
	@ValidationMethod(message = "Single column date range (set via column) and two column date range (set via startColumn and endColumn) are exclusive.")
	default boolean isExclusiveValidityDates() {
		if (getColumn() == null) {
			return getStartColumn() != null && getEndColumn() != null;
		}
		return getStartColumn() == null && getEndColumn() == null;
	}

	@JsonIgnore
	@ValidationMethod(message = "Start and end column must be part of the same table.")
	default boolean isOfSameTable() {
		if (getColumn() != null) {
			return true;
		}
		return getStartColumn().getTable() == getEndColumn().getTable();
	}

	@JsonIgnore
	@ValidationMethod(message = "Both columns of a two-column daterange have to be of type DATE.")
	default boolean isValidTwoColumnDaterange() {
		if (getStartColumn() == null || getEndColumn() == null) {
			return true;
		}
		return getStartColumn().resolve().getType() == MajorTypeId.DATE && getEndColumn().resolve().getType() == MajorTypeId.DATE;
	}

	@JsonIgnore
	@ValidationMethod(message = "Column is not of type DATE or DATE_RANGE.")
	default boolean isValidValidityDatesSingleColumn() {
		if (getColumn() == null) {
			return true;
		}
		return getColumn().resolve().getType().isDateCompatible();
	}

}
