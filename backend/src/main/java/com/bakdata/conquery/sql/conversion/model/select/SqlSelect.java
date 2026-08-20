package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import java.util.List;

import com.bakdata.conquery.sql.conversion.model.Qualifiable;
import org.jooq.Field;

public interface SqlSelect extends Qualifiable<SqlSelect> {

	List<Field<?>> toFields();

	/**
	 * All column names this {@link SqlSelect} requires.
	 */
	List<String> requiredColumns();

	/**
	 * @return Determines if this SqlSelect is only part of the final concept conversion CTE and has no predeceasing selects.
	 */
	default boolean isUniversal() {
		return false;
	}

	/**
	 * Special selects like {@link ExistsSelect} require an extra aggregation when joining them with other connectors.
	 */
	default SqlSelect connectorAggregate() {
		return this;
	}

	/**
	 * Special selects like {@link ExistsSelect} require to be converted into a specific format before executing the
	 * final query.
	 */
	default SqlSelect toFinalRepresentation() {
		return this;
	}

}
