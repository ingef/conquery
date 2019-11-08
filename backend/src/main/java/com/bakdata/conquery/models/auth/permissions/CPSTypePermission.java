package com.bakdata.conquery.models.auth.permissions;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;


public abstract class CPSTypePermission extends ConqueryPermission implements HasTarget {
	
	@JsonIgnore
	private final Class<?> cpsBaseClass;
	@Getter
	private final String target;
	
	@JsonCreator
	public CPSTypePermission(Class<?> base, String typeName) {
		cpsBaseClass = base;
		Set<?> implementations = CPSTypeIdResolver.listImplementations(base);
		if(implementations.isEmpty()) {
			throw new IllegalArgumentException(String.format("Base class does not have any implementations",base));
		}
		if(!implementations
			.stream()
			.map(c -> ((Class<?>)c).getAnnotation(CPSType.class).id())
			.anyMatch(id ->id.equals(typeName))) {
			throw new IllegalArgumentException((String.format("Provided class is not a CPSType of base %s.", base)));
		}
		target = typeName;
	}
	
	public CPSTypePermission(Class<?> clazz) {
		CPSType anno = clazz.getAnnotation(CPSType.class);
		if(anno == null) {
			throw new IllegalArgumentException("Provided class is not a CPSType");
		}
		cpsBaseClass = anno.base();
		target = anno.id();
	}

	@Override
	public Optional<ConqueryPermission> findSimilar(Collection<ConqueryPermission> permissions) {
		Iterator<ConqueryPermission> it = permissions.iterator();
		while (it.hasNext()) {
			ConqueryPermission perm = it.next();
			if(!getClass().isAssignableFrom(perm.getClass())) {
				continue;
			}
			if (!((HasTarget)perm).getTarget().equals(getTarget())) {
				continue;
			}
			return Optional.of(perm);
		}
		return Optional.empty();
	}
}
