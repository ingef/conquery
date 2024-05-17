package com.bakdata.conquery.models.datasets.concepts;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;

public interface DaterangeSelectOrFilter {

	Column getColumn();

	Column getStartColumn();

	Column getEndColumn();

	default boolean isSingleColumnDaterange() {
		return getColumn() != null;
	}

	@JsonIgnore
	default Table getTable() {
		if (getColumn() != null) {
			return getColumn().getTable();
		}
		// start and end column are of the same table, so it does not matter which one we choose
		return getStartColumn().getTable();
	}

	@JsonIgnore
	@ValidationMethod(message = "Single column date range (set via column) and two column date range (set via startColumn and endColumn) are exclusive.")
	default boolean isExclusiveDateRange() {
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
	default boolean isValidTwoColumnDaterangeSelect() {
		if (getStartColumn() == null || getEndColumn() == null) {
			return true;
		}
		return getStartColumn().getType() == MajorTypeId.DATE && getEndColumn().getType() == MajorTypeId.DATE;
	}

	@JsonIgnore
	@ValidationMethod(message = "Column is not of type DATE or DATE_RANGE.")
	default boolean isValidSingleColumnDateSelect() {
		if (getColumn() == null) {
			return true;
		}
		return getColumn().getType().isDateCompatible();
	}

}
