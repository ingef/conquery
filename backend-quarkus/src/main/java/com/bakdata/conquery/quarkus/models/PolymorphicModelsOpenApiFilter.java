package com.bakdata.conquery.quarkus.models;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.arc.Arc;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Schema;

/**
 * Aligns the generated OpenAPI document with the runtime Jackson model.
 * <p>
 * Vanilla SmallRye OpenAPI generation cannot fully describe the model for two reasons:
 * <ul>
 *     <li>Polymorphic implementations are registered dynamically through CDI providers. The static OpenAPI scan does
 *     not know which implementations belong to a base type, so it cannot build the corresponding {@code oneOf} and
 *     discriminator mappings.</li>
 *     <li>Some value objects, notably IDs, are structured records in Java but use a delegating {@code @JsonCreator}
 *     and {@code @JsonValue} to deserialize and serialize as scalars. SmallRye otherwise exposes their internal record
 *     components instead of their JSON representation.</li>
 * </ul>
 * This filter projects the runtime polymorphic registry into OpenAPI and replaces affected component schemas with the
 * scalar type used by Jackson. As a result, generated clients see the same shapes that the API actually accepts and
 * returns.
 */
@OpenApiFilter(OpenApiFilter.RunStage.RUN)
public class PolymorphicModelsOpenApiFilter implements OASFilter {

	@Override
	public void filterOpenAPI(OpenAPI openAPI) {
		PolymorphicModelRegistry registry = Arc.container().instance(PolymorphicModelRegistry.class).get();
		Components components = openAPI.getComponents();
		if (components == null) {
			components = OASFactory.createComponents();
			openAPI.setComponents(components);
		}

		Map<Class<?>, List<PolymorphicModelType<?, ?>>> byBase = new LinkedHashMap<>();
		registry.allTypes().forEach(type -> byBase.computeIfAbsent(type.baseType(), ignored -> new ArrayList<>()).add(type));
		for (Map.Entry<Class<?>, List<PolymorphicModelType<?, ?>>> family : byBase.entrySet()) {
			addFamilySchema(components, family.getKey(), family.getValue());
		}
		replaceJacksonScalarSchemas(components, registry.allTypes());
	}

	private void replaceJacksonScalarSchemas(Components components, Collection<PolymorphicModelType<?, ?>> types) {
		Set<Class<?>> visited = new HashSet<>();
		for (PolymorphicModelType<?, ?> type : types) {
			visitReferencedTypes(components, type.modelType(), visited);
		}
	}

	private void visitReferencedTypes(Components components, Type type, Set<Class<?>> visited) {
		if (type instanceof ParameterizedType parameterizedType) {
			visitReferencedTypes(components, parameterizedType.getRawType(), visited);
			for (Type argument : parameterizedType.getActualTypeArguments()) {
				visitReferencedTypes(components, argument, visited);
			}
			return;
		}
		if (!(type instanceof Class<?> modelType) || modelType.isPrimitive() || modelType.isArray() || !visited.add(modelType)) {
			return;
		}

		Optional<Class<?>> scalarType = jacksonScalarType(modelType);
		if (scalarType.isPresent()) {
			replaceComponentSchema(components, modelType, scalarType.get());
			return;
		}
		if (!modelType.getPackageName().startsWith("com.bakdata.conquery")) {
			return;
		}

		for (RecordComponent component : modelType.getRecordComponents() == null ? new RecordComponent[0] : modelType.getRecordComponents()) {
			visitReferencedTypes(components, component.getGenericType(), visited);
		}
		for (Field field : modelType.getDeclaredFields()) {
			if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
				visitReferencedTypes(components, field.getGenericType(), visited);
			}
		}
		Class<?> superType = modelType.getSuperclass();
		if (superType != null) {
			visitReferencedTypes(components, superType, visited);
		}
	}

	private Optional<Class<?>> jacksonScalarType(Class<?> modelType) {
		Optional<Class<?>> creatorType = delegatingCreatorType(modelType);
		Optional<Class<?>> jsonValueType = jsonValueType(modelType);
		if (creatorType.isEmpty() || jsonValueType.isEmpty()) {
			return Optional.empty();
		}
		if (!creatorType.get().equals(jsonValueType.get())) {
			throw new IllegalStateException("Jackson scalar model " + modelType.getName()
					+ " uses incompatible @JsonCreator and @JsonValue types: "
					+ creatorType.get().getName() + " and " + jsonValueType.get().getName());
		}
		return creatorType;
	}

	private Optional<Class<?>> delegatingCreatorType(Class<?> modelType) {
		for (Method method : modelType.getDeclaredMethods()) {
			JsonCreator creator = method.getAnnotation(JsonCreator.class);
			if (isDelegatingCreator(creator, method.getParameterCount())) {
				return Optional.of(method.getParameterTypes()[0]);
			}
		}
		for (Constructor<?> constructor : modelType.getDeclaredConstructors()) {
			JsonCreator creator = constructor.getAnnotation(JsonCreator.class);
			if (isDelegatingCreator(creator, constructor.getParameterCount())) {
				return Optional.of(constructor.getParameterTypes()[0]);
			}
		}
		return Optional.empty();
	}

	private boolean isDelegatingCreator(JsonCreator creator, int parameterCount) {
		return creator != null && creator.mode() == JsonCreator.Mode.DELEGATING && parameterCount == 1;
	}

	private Optional<Class<?>> jsonValueType(Class<?> modelType) {
		for (Method method : modelType.getDeclaredMethods()) {
			if (isJsonValue(method)) {
				return Optional.of(method.getReturnType());
			}
		}
		for (Field field : modelType.getDeclaredFields()) {
			if (isJsonValue(field)) {
				return Optional.of(field.getType());
			}
		}
		return Optional.empty();
	}

	private boolean isJsonValue(AnnotatedElement element) {
		JsonValue annotation = element.getAnnotation(JsonValue.class);
		return annotation != null && annotation.value();
	}

	private void replaceComponentSchema(Components components, Class<?> modelType, Class<?> scalarType) {
		if (components.getSchemas() == null) {
			return;
		}
		String name = schemaName(modelType);
		Schema existing = components.getSchemas().get(name);
		if (existing == null) {
			return;
		}
		Schema replacement = OASFactory.createSchema().addType(openApiScalarType(modelType, scalarType));
		if (existing.getDescription() != null) {
			replacement.description(existing.getDescription());
		}
		components.addSchema(name, replacement);
	}

	private Schema.SchemaType openApiScalarType(Class<?> modelType, Class<?> scalarType) {
		if (scalarType == String.class || scalarType == char.class || scalarType == Character.class) {
			return Schema.SchemaType.STRING;
		}
		if (scalarType == boolean.class || scalarType == Boolean.class) {
			return Schema.SchemaType.BOOLEAN;
		}
		if (scalarType == byte.class || scalarType == Byte.class
				|| scalarType == short.class || scalarType == Short.class
				|| scalarType == int.class || scalarType == Integer.class
				|| scalarType == long.class || scalarType == Long.class) {
			return Schema.SchemaType.INTEGER;
		}
		if (Number.class.isAssignableFrom(scalarType) || scalarType == float.class || scalarType == double.class) {
			return Schema.SchemaType.NUMBER;
		}
		throw new IllegalStateException("Unsupported @JsonCreator scalar type " + scalarType.getName() + " on " + modelType.getName());
	}

	private void addFamilySchema(Components components, Class<?> baseType, List<PolymorphicModelType<?, ?>> types) {
		FamilySchema family = familySchema(baseType);
		Discriminator discriminator = OASFactory.createDiscriminator().propertyName(family.discriminator());
		List<Schema> alternatives = new ArrayList<>();
		for (PolymorphicModelType<?, ?> type : types) {
			String schemaName = schemaName(type.modelType());
			Schema concreteSchema = components.getSchemas() == null ? null : components.getSchemas().get(schemaName);
			if (concreteSchema == null) {
				throw new IllegalStateException("No generated OpenAPI schema found for registered polymorphic model " + type.modelType().getName() + ". Annotate and index the model class so SmallRye OpenAPI can discover it.");
			}
			concreteSchema.addProperty(family.discriminator(), OASFactory.createSchema()
					.addType(Schema.SchemaType.STRING)
					.constValue(type.typeId()));
			concreteSchema.addRequired(family.discriminator());
			String reference = "#/components/schemas/" + schemaName;
			discriminator.addMapping(type.typeId(), reference);
			alternatives.add(OASFactory.createSchema().ref(reference));
		}
		Schema familySchema = OASFactory.createSchema()
				.description(family.description())
				.discriminator(discriminator)
				.oneOf(List.copyOf(alternatives));
		components.addSchema(family.schemaName(), familySchema);
	}

	private FamilySchema familySchema(Class<?> baseType) {
		PolymorphicModelBase internal = baseType.getAnnotation(PolymorphicModelBase.class);
		if (internal != null) {
			return new FamilySchema(internal.discriminator(), internal.schemaName(), internal.description());
		}
		com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelBase plugin =
				baseType.getAnnotation(com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelBase.class);
		if (plugin != null) {
			return new FamilySchema(plugin.discriminator(), plugin.schemaName(), plugin.description());
		}
		throw new IllegalStateException("Polymorphic model base " + baseType.getName() + " is not annotated");
	}

	private record FamilySchema(String discriminator, String schemaName, String description) {
	}

	private String schemaName(Class<?> modelType) {
		org.eclipse.microprofile.openapi.annotations.media.Schema annotation = modelType.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);
		if (annotation != null && !annotation.name().isBlank()) {
			return annotation.name();
		}
		return modelType.getSimpleName();
	}
}
