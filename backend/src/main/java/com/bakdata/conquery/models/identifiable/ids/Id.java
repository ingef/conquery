package com.bakdata.conquery.models.identifiable.ids;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@JsonDeserialize(using = IdDeserializer.class)
public abstract class Id<TYPE> {

	private static final LoadingCache<Id<?>, String>
			ESCAPED_IDS =
			CacheBuilder.newBuilder().maximumSize(10_000).expireAfterAccess(2, TimeUnit.DAYS).build(new CacheLoader<Id<?>, String>() {
				@Override
				public String load(Id<?> key) throws Exception {
					return key.escapedIdString();
				}
			});

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	@Override
	@JsonValue
	public String toString() {
		try {
			return ESCAPED_IDS.get(this);
		}
		catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private final String escapedIdString() {
		List<Object> components = getComponents();
		components.replaceAll(o -> ConqueryEscape.escape(Objects.toString(o)));
		return IdUtil.JOINER.join(components);
	}

	public final List<Object> getComponents() {
		List<Object> components = new ArrayList<>();
		this.collectComponents(components);
		return components;
	}

	public abstract void collectComponents(List<Object> components);

	public final List<String> collectComponents() {
		List<Object> components = getComponents();
		List<String> result = new ArrayList<>(components.size());

		for (Object component : components) {
			result.add(ConqueryEscape.escape(Objects.toString(component)));
		}

		return result;
	}
}
