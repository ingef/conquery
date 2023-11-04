package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.select.Select;

/**
 * A {@link SqlSelect} a user explicitly requested by a choosing a {@link Concept}s {@link Select}.
 */
public interface ExplicitSelect extends SqlSelect {

	/**
	 * Identifies this {@link SqlSelect} with a unique reference to a {@link Concept}s {@link Select}.
	 */
	SqlSelectId getSqlSelectId();

	@Override
	default <T extends SqlSelect> T createReference(String qualifier, Class<T> selectClass) {
		return selectClass.cast(
				ExplicitExtractingSelect.fromSqlSelect(
						getSqlSelectId(),
						qualifier,
						columnName(),
						aliased().getType()
				)
		);
	}
}
