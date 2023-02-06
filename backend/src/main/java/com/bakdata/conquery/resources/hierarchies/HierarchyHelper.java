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

/**
 * UriBuilder helper class, that provides an convenience method {@link HierarchyHelper#hierarchicalPath(UriBuilder, Class, String)}
 * to retrieve the full path to an endpoint (defined by a method with an {@link GET}/{@link DELETE}/... annotation),
 * by concatenating {@link Path}-annotations found on the method and the enclosing class.
 *
 * A call to {@code HierarchyHelper.hierarchicalPath(uriBuilder, c, "m") } is similar to
 * {@code uriBuilder.path(c).path(c, "m")}. However the {@link HierarchyHelper} would not fail if the {@link Path}
 * annotation is only present on either of these.
 */

@UtilityClass
public final class HierarchyHelper {

	/**
	 * Appends a path to the UriBuilder according to the {@link Path} annotations on class and/or method.
	 * The method must be an endpoint and as such be annotated with {@link GET}/{@link DELETE}/... .
	 * @throws IllegalArgumentException if the method is not an endpoint or no {@link Path} annotation can be found.
	 */
	public static UriBuilder hierarchicalPath(UriBuilder uri, Class<?> clazz, String methodName) {
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
		
		boolean foundPath = false;

		if (clazz.isAnnotationPresent(Path.class)) {
			uri.path(clazz);
			foundPath = true;
		}

		if (foundMethod.isAnnotationPresent(Path.class)) {
			uri.path(clazz, methodName);
			foundPath = true;
		}
		
		if (!foundPath) {
			throw new IllegalArgumentException("The javax.ws.rs.Path annotation was present neither on the class '"+ clazz.getName() + "' nor on the method " + methodName + "'.");
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
