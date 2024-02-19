package com.bakdata.conquery.models.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.query.entity.Entity;
import lombok.NonNull;
import lombok.ToString;

/**
 * Helper class for {@link com.bakdata.conquery.apiv1.query.Query#collectRequiredEntities(QueryExecutionContext)}, encapsulating logic.
 */
@ToString(onlyExplicitlyIncluded = true)
public final class RequiredEntities {

	private final Set<String> entities;

	public RequiredEntities() {
		this(new HashSet<>());
	}

	public RequiredEntities(Collection<String> entities) {
		this.entities = new HashSet<>(entities);
	}

	public RequiredEntities intersect(@NonNull RequiredEntities other) {
		final Set<String> out = new HashSet<>(entities);
		out.retainAll(other.entities);

		return new RequiredEntities(out);
	}

	public RequiredEntities union(@NonNull RequiredEntities other) {
		final Set<String> out = new HashSet<>(entities);
		out.addAll(other.entities);

		return new RequiredEntities(out);
	}

	public Set<Entity> resolve(BucketManager bucketManager) {
		final Set<String> all = bucketManager.getEntity2Bucket().keySet();
		return entities.stream()
					   .filter(all::contains)
					   // The following is just a wrapping, that is later unwrapped again in an execution
					   .map(Entity::new)
					   .collect(Collectors.toSet());
	}
}
