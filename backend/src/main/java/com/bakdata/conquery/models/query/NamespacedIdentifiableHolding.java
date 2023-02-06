package com.bakdata.conquery.models.query;

import java.util.Set;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;

/**
 * Classes that contain {@link NamespacedIdentifiable}s on their first level should implement this interface.
 * It can be used by the Visitor pattern that is implemented by {@link ManagedExecution} to collect all ids
 * that were supplied by a User before the structure was initialized.
 * These ids are than used to e.g. check permissions.
 */
public interface NamespacedIdentifiableHolding {

	void collectNamespacedObjects(Set<NamespacedIdentifiable<?>> identifiables);
}
