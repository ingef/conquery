package com.bakdata.conquery.models.identifiable.ids;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AId<TYPE> implements IId<TYPE> {

	@Override
	public abstract boolean equals(Object obj);
	
	@Override
	public abstract int hashCode();
	
	/*@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AId<TYPE> other = (AId<TYPE>) obj;
		List<Object> components = new ArrayList<>();
		this.collectComponents(components);
		List<Object> objComponents = new ArrayList<>(components.size());
		other.collectComponents(objComponents);
		return components.equals(objComponents);
	}
	
	@Override
	public int hashCode() {
		List<Object> components = new ArrayList<>();
		this.collectComponents(components);
		return components.hashCode();
	}*/
	
	@Override @JsonValue
	public String toString() {
		List<Object> components = new ArrayList<>();
		this.collectComponents(components);
		return IId.JOINER.join(components);
	}

	public abstract void collectComponents(List<Object> components);
}
