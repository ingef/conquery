package com.bakdata.conquery.models.query.concept;

import java.util.Set;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

/**
 * Classes that contain {@link NamespacedId}s on their first level should implement this interface.
 * It can be used by the Visitor pattern that is implemented by {@link ManagedExecution} to collect all ids
 * that were supplied by a User before the structure was initialized.
 * These ids are than used to e.g. check permissions.
 */
public interface NamespacedIdHolding {

	void collectNamespacedIds(Set<NamespacedId> ids);
}
