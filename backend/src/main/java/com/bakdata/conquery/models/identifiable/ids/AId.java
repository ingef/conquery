package com.bakdata.conquery.models.identifiable.ids;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AId<TYPE> implements IId<TYPE> {

	@Override
	public abstract boolean equals(Object obj);
	
	@Override
	public abstract int hashCode();
	
	@Override @JsonValue
	public String toString() {
		List<Object> components = getComponents();
		components.replaceAll(o->ConqueryEscape.escape(Objects.toString(o)));
		return IId.JOINER.join(components);
	}

	public List<Object> getComponents() {
		List<Object> components = new ArrayList<>();
		this.collectComponents(components);
		return components;
	}

	public abstract void collectComponents(List<Object> components);
}
