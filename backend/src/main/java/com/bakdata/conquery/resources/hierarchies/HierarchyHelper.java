package com.bakdata.conquery.resources.hierarchies;

import java.lang.reflect.Method;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class HierarchyHelper {

	public static UriBuilder fromHierachicalPathResourceMethod(String base, Class<?> clazz, String methodName) {
		assert (clazz != null);
		assert (methodName != null);

		Method[] methods = clazz.getMethods();
		Method foundMethod = null;
		for (Method method : methods) {
			if (!method.getName().equals(methodName)) {
				continue;
			}
			if (isEndpoint(method)) {
				foundMethod = method;
				break;
			}
		}

		if (foundMethod == null) {
			throw new IllegalArgumentException(
				String.format("Method %s not found or is not annotated as HttpMethod in class %s", methodName, clazz));
		}

		UriBuilder uri = UriBuilder.fromPath(base);
		Class<?> currentClass = clazz;
		do {
			// Walk up the class hierarchy and collect @Path annotations
			try {
				uri.path(currentClass);
			}
			catch (IllegalArgumentException e) {
				// ignore this class, a @Path might be more up in the hierarchy
			}
			currentClass = currentClass.getSuperclass();
		} while (!currentClass.equals(Object.class));
		if (foundMethod.isAnnotationPresent(Path.class)) {
			uri.path(clazz, methodName);
		}
		return uri;
	}

	private static boolean isEndpoint(Method method) {
		return method.isAnnotationPresent(GET.class)
			|| method.isAnnotationPresent(DELETE.class)
			|| method.isAnnotationPresent(HEAD.class)
			|| method.isAnnotationPresent(OPTIONS.class)
			|| method.isAnnotationPresent(POST.class)
			|| method.isAnnotationPresent(PUT.class);
	}
}
