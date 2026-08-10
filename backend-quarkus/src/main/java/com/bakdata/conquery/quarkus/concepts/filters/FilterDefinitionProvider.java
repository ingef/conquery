package com.bakdata.conquery.quarkus.concepts.filters;

import java.util.Set;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

/**
 * Registers and assembles one polymorphic metadata filter definition.
 * <p>
 * Implementations are CDI beans. The polymorphic model registry uses {@link #modelType()} and its
 * {@code @PolymorphicModelSubtype} annotation to
 * configure Jackson and OpenAPI, while the metadata loader uses {@link #convert(FilterConversionContext,
 * FilterDefinition)} to turn the file-facing definition into the catalog model exposed to the frontend.
 *
 * @param <T> concrete filter definition handled by this provider
 */
public interface FilterDefinitionProvider<T extends FilterDefinition> extends PolymorphicModelTypeProvider<FilterDefinition, T> {

	/**
	 * @return discriminator used in metadata JSON to select this provider
	 */
	default String type() {
		return typeId();
	}

	/**
	 * Declares every query value model that filters produced by this provider may accept. All returned classes must
	 * have a registered filter value provider. The assembler verifies that the serialized type of the converted filter
	 * belongs to this set.
	 *
	 * @return non-empty set of accepted filter value model classes
	 */
	Set<Class<? extends FilterValue>> acceptedValueTypes();

	/**
	 * Converts a deserialized metadata definition into the catalog filter consumed by the API and frontend.
	 * <p>
	 * The payload has already been selected by its discriminator and cast to {@link #modelType()}. Metadata bean
	 * validation is performed before assembly. Implementations remain responsible for semantic conversion, including
	 * resolving local column names and constructing hierarchical IDs through the supplied context.
	 * <p>
	 * The returned filter must be complete and non-null. Its serialized filter value type must correspond to a
	 * registered filter value provider for one of {@link #acceptedValueTypes()}; violating this invariant aborts metadata
	 * loading. Invalid references or unsupported metadata should likewise be reported by throwing an exception with
	 * enough context to identify the definition.
	 *
	 * @param context connector, table, column-resolution, and ID-fallback context for this definition
	 * @param payload concrete file-facing definition to convert
	 * @return fully assembled catalog filter
	 */
	DatasetCatalogRepository.Filter convert(FilterConversionContext context, T payload);

}
