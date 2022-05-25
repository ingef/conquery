package com.bakdata.conquery.models.identifiable.ids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@JsonDeserialize(using = IdDeserializer.class)
public abstract class Id<TYPE> {

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	@Override
	@JsonValue
	public String toString() {
		List<Object> components = getComponents();
		components.replaceAll(o -> ConqueryEscape.escape(Objects.toString(o)));
		return IdUtil.JOINER.join(components);
	}

	public List<Object> getComponents() {
		List<Object> components = new ArrayList<>();
		this.collectComponents(components);
		return components;
	}

	public abstract void collectComponents(List<Object> components);

	public List<String> collectComponents() {
		List<Object> components = getComponents();
		String[] result = new String[components.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = ConqueryEscape.escape(Objects.toString(components.get(i)));
		}
		return Arrays.asList(result);
	}
}
