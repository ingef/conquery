package com.bakdata.conquery.sql.conversion.model.select;

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

}
