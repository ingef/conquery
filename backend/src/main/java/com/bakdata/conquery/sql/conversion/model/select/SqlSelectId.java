package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Value;

@Value
public class SqlSelectId {

	/**
	 * The name (name or label) of the corresponding {@link Select} in the {@link Concept}s definition.
	 */
	String name;

	/**
	 * The result type of the corresponding {@link Select}.
	 */
	ResultType resultType;

	public static SqlSelectId fromSelect(Select select) {
		return new SqlSelectId(select.getColumnName(), select.getResultType());
	}

}
