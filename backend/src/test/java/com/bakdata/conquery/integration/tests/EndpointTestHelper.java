package com.bakdata.conquery.integration.tests;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.jackson.Jackson;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.databind.ObjectReader;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

/**
 * The following functions are adapted from dropwizards
 * {@link DropwizardResourceConfig.EndpointLogger } class.
 */
public class EndpointTestHelper {

	public static final ObjectReader READER = Jackson.getMapper()
													 .readerFor(Jackson.getMapper().getTypeFactory().constructCollectionLikeType(List.class, EndPoint.class));

	private static final TypeResolver TYPE_RESOLVER = new TypeResolver();

	@AllArgsConstructor
	@Getter
	@EqualsAndHashCode
	public static class EndPoint {

		private String method;
		private String path;
		private String clazz;

		public String toString() {
			return String.format("%-8s%-25s%s", method, clazz, path);
		}
	}

	public static void populate(Class<?> klass, List<EndPoint> endpointLogLines) {
		populate("/", klass, false, endpointLogLines);
	}

	private static void populate(String basePath, Class<?> klass, boolean isLocator, List<EndPoint> endpointLogLines) {
		populate(basePath, klass, isLocator, Resource.from(klass), endpointLogLines);
	}

	private static void populate(String basePath, Class<?> klass, boolean isLocator, Resource resource, List<EndPoint> endpointLogLines) {
		if (!isLocator) {
			basePath = normalizePath(basePath, resource.getPath());
		}

		for (ResourceMethod method : resource.getResourceMethods()) {
			endpointLogLines.add(new EndPoint(method.getHttpMethod(), basePath, klass.getSimpleName()));
		}

		for (Resource childResource : resource.getChildResources()) {
			for (ResourceMethod method : childResource.getAllMethods()) {
				if (method.getType() == ResourceMethod.JaxrsType.RESOURCE_METHOD) {
					final String path = normalizePath(basePath, childResource.getPath());
					endpointLogLines.add(new EndPoint(method.getHttpMethod(), path, klass.getSimpleName()));
				}
				else if (method.getType() == ResourceMethod.JaxrsType.SUB_RESOURCE_LOCATOR) {
					final String path = normalizePath(basePath, childResource.getPath());
					final ResolvedType responseType = TYPE_RESOLVER.resolve(method.getInvocable().getResponseType());
					final Class<?> erasedType = !responseType.getTypeBindings().isEmpty()
						? responseType.getTypeBindings().getBoundType(0).getErasedType()
						: responseType.getErasedType();
					if (Resource.from(erasedType) == null) {
						endpointLogLines.add(new EndPoint(method.getHttpMethod(), path, erasedType.getSimpleName()));
					}
					else {
						populate(path, erasedType, true, endpointLogLines);
					}
				}
			}
		}
	}

	private static String normalizePath(String basePath, String path) {
		if (path == null) {
			return basePath;
		}
		if (basePath.endsWith("/")) {
			return path.startsWith("/") ? basePath + path.substring(1) : basePath + path;
		}
		return path.startsWith("/") ? basePath + path : basePath + "/" + path;
	}

	public static List<EndPoint> collectEndpoints(DropwizardResourceConfig jerseyConfig) {
		// Collect all classes
		List<Class<?>> classes = new ArrayList<>(jerseyConfig.getClasses());
		for (Object singleton : jerseyConfig.getSingletons()) {
			classes.add(singleton.getClass());
		}

		// Get the resources
		List<Class<?>> allResourcesClasses = new ArrayList<>();
		List<EndPoint> resources = new ArrayList<>();
		for (Class<?> clazz : classes) {
			if (!clazz.isInterface() && Resource.from(clazz) != null) {
				allResourcesClasses.add(clazz);
			}
		}

		// Prepare endpoints for comparison
		allResourcesClasses.sort((e1, e2) -> e1.getName().compareTo(e2.getName()));
		for (Class<?> clazz : allResourcesClasses) {
			EndpointTestHelper.populate(clazz, resources);
		}
		return resources;
	}
}
