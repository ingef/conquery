package com.bakdata.conquery.quarkus.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.arc.Arc;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Schema;

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
	}

	private void addFamilySchema(Components components, Class<?> baseType, List<PolymorphicModelType<?, ?>> types) {
		PolymorphicModelBase base = baseType.getAnnotation(PolymorphicModelBase.class);
		Discriminator discriminator = OASFactory.createDiscriminator().propertyName(base.discriminator());
		List<Schema> alternatives = new ArrayList<>();
		for (PolymorphicModelType<?, ?> type : types) {
			String schemaName = schemaName(type.modelType());
			Schema concreteSchema = components.getSchemas() == null ? null : components.getSchemas().get(schemaName);
			if (concreteSchema == null) {
				throw new IllegalStateException("No generated OpenAPI schema found for registered polymorphic model " + type.modelType().getName() + ". Annotate and index the model class so SmallRye OpenAPI can discover it.");
			}
			concreteSchema.addProperty(base.discriminator(), OASFactory.createSchema()
					.addType(Schema.SchemaType.STRING)
					.constValue(type.typeId()));
			concreteSchema.addRequired(base.discriminator());
			String reference = "#/components/schemas/" + schemaName;
			discriminator.addMapping(type.typeId(), reference);
			alternatives.add(OASFactory.createSchema().ref(reference));
		}
		Schema familySchema = OASFactory.createSchema()
				.description(base.description())
				.discriminator(discriminator)
				.oneOf(List.copyOf(alternatives));
		components.addSchema(base.schemaName(), familySchema);
	}

	private String schemaName(Class<?> modelType) {
		org.eclipse.microprofile.openapi.annotations.media.Schema annotation = modelType.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);
		if (annotation != null && !annotation.name().isBlank()) {
			return annotation.name();
		}
		return modelType.getSimpleName();
	}
}
