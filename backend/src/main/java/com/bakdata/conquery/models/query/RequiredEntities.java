package com.bakdata.conquery.models.query;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.query.entity.Entity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.NonNull;

public sealed interface RequiredEntities {
	RequiredEntities and(RequiredEntities other);

	RequiredEntities or(RequiredEntities other);

	Set<Entity> resolve(BucketManager bucketManager);


	static final class Some implements RequiredEntities {

		private final IntSet entities;

		public Some() {
			this(new IntOpenHashSet());
		}

		public Some(IntCollection entities) {
			this.entities = new IntOpenHashSet(entities);
		}

		@Override
		public RequiredEntities and(@NonNull RequiredEntities other) {
			if (other instanceof Some someOther) {
				final IntOpenHashSet out = new IntOpenHashSet(entities);
				out.retainAll(someOther.entities);

				return new Some(out);
			}

			throw new IllegalArgumentException("Impossible");
		}

		@Override
		public RequiredEntities or(RequiredEntities other) {


			if (other instanceof Some someOther) {
				final IntOpenHashSet out = new IntOpenHashSet(entities);
				out.addAll(someOther.entities);

				return new Some(out);
			}

			throw new IllegalArgumentException("Impossible");
		}

		public RequiredEntities drop(RequiredEntities others) {
			final IntOpenHashSet out = new IntOpenHashSet(entities);

			if(others instanceof Some some) {
				out.removeAll(some.entities);
			}

			return new Some(out);
		}

		@Override
		public Set<Entity> resolve(BucketManager bucketManager) {
			final Int2ObjectMap<Entity> all = bucketManager.getEntities();
			return entities.intStream().mapToObj(all::get).filter(Objects::nonNull).collect(Collectors.toSet());
		}
	}
}
