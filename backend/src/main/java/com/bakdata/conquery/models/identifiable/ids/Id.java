package com.bakdata.conquery.models.identifiable.ids;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@JsonDeserialize(using = IdDeserializer.class)
public abstract class Id<TYPE> {

	@JsonIgnore
	private WeakReference<String> escapedId;

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	@Override
	@JsonValue
	public final String toString() {
		final String escaped;
		if (escapedId != null && (escaped = escapedId.get()) != null) {
			return escaped;
		}

		String escapedIdString = escapedIdString();
		escapedId = new WeakReference<>(escapedIdString);
		return escapedIdString;
	}

	private String escapedIdString() {
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
