package com.bakdata.conquery.models.query;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.query.entity.Entity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.NonNull;
import lombok.ToString;

@ToString(onlyExplicitlyIncluded = true)
public final class RequiredEntities {

	private final IntSet entities;

	public RequiredEntities() {
		this(new IntOpenHashSet());
	}

	public RequiredEntities(Collection<Integer> entities) {
		this.entities = new IntOpenHashSet(entities);
	}

	public RequiredEntities and(@NonNull RequiredEntities other) {
		final IntOpenHashSet out = new IntOpenHashSet(entities);
		out.retainAll(other.entities);

		return new RequiredEntities(out);
	}

	public RequiredEntities or(@NonNull RequiredEntities other) {
		final IntOpenHashSet out = new IntOpenHashSet(entities);
		out.addAll(other.entities);

		return new RequiredEntities(out);
	}

	public Set<Entity> resolve(BucketManager bucketManager) {
		final Int2ObjectMap<Entity> all = bucketManager.getEntities();
		return entities.intStream()
					   .mapToObj(all::get)
					   .filter(Objects::nonNull)
					   .collect(Collectors.toSet());
	}
}
