package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.models.query.QueryResolveContext;

/**
 * Additional interface used objects that are received by a
 * {@link com.bakdata.conquery.models.datasets.concepts.filters.GroupFilter} to mark them as resolvable.
 * <p>
 * Resolving usually processes the submitted data in the object
 * (that is also persisted) and adds transient data that is later
 * needed during query execution. So pay attention that
 * the submitted data stays untouched and any produced data is held in
 * transient field that are only serialized, deserialized in the communication between
 * manager and shards but not persisted.
 */
public interface QueryContextResolvable {

	void resolve(QueryResolveContext context);

}