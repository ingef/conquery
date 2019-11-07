package com.bakdata.conquery.models.auth.permissions;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;


public abstract class CPSTypePermission extends ConqueryPermission implements HasTarget {
		
	@Getter
	private final String target;
	
	@JsonCreator
	public CPSTypePermission(String typeName) {
		target = typeName;
	}
	
	public CPSTypePermission(Class<?> clazz) {
		CPSType anno = clazz.getAnnotation(CPSType.class);
		if(anno == null) {
			throw new IllegalStateException("Provided class is not a CPSType");
		}
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


//	protected static Set<Class<?>> createAllowedBaseClasses(Class<?> ...classes) {
//		Set<Class<?>> childClasses = new HashSet<>();
//		for(Class<?> clazz : classes) {
//			childClasses.addAll(CPSTypeIdResolver.listImplementations(clazz));
//		}
//		return childClasses;
//	}
//	
//	protected abstract Set<Class<?>> getAllowedBaseClasses();

}
