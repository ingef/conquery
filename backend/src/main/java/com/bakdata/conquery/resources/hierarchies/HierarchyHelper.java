package com.bakdata.conquery.resources.hierarchies;

import javax.ws.rs.core.UriBuilder;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class HierarchyHelper {

	
	public static UriBuilder fromHierachicalPathResourceMethod(String base, Class<?> clazz, String methodName) {
		UriBuilder uri = UriBuilder.fromPath(base);
		Class<?> currentClass = clazz;
		do {
			// Walk up the class hierarchy and collect @Path annotations
			try {
				uri.path(currentClass);	
			} catch (IllegalArgumentException e) {
				// ignore this class, a @Path might be more up in the hierarchy
			}
			currentClass = currentClass.getSuperclass();
		}while(!currentClass.equals(Object.class));
		uri.path(clazz, methodName);
		return uri;
	}
}
