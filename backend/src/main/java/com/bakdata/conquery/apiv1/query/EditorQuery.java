package com.bakdata.conquery.apiv1.query;

import com.bakdata.conquery.apiv1.execution.ExecutionStatus;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.sql.conquery.SqlManagedQuery;

/**
 * Common abstraction for intersecting parts of {@link ManagedQuery} and {@link SqlManagedQuery}.
 */
public interface EditorQuery {

	Query getQuery();

	Long getLastResultCount();

	default void enrichStatusBase(ExecutionStatus status) {
		status.setNumberOfResults(getLastResultCount());

		Query query = getQuery();
		status.setQueryType(query.getClass().getAnnotation(CPSType.class).id());

		if (query instanceof SecondaryIdQuery) {
			status.setSecondaryId(((SecondaryIdQuery) query).getSecondaryId().getId());
		}
	}

}
