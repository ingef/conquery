package com.bakdata.conquery.quarkus.plugin.api.filters;

import java.util.Set;

import com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelTypeProvider;

/**
 * CDI discovery and conversion contract for one metadata filter model.
 *
 * @param <T> concrete, annotated filter definition contributed by the plugin
 */
public interface FilterDefinitionProvider<T extends FilterDefinition> extends PolymorphicModelTypeProvider<FilterDefinition, T> {

	default String type() {
		return typeId();
	}

	/**
	 * Declares the frontend filter-value discriminator IDs emitted by this provider.
	 *
	 * @return non-empty set of registered IDs such as {@code STRING} or {@code INTEGER_RANGE}
	 */
	Set<String> acceptedValueTypes();

	/**
	 * Converts validated plugin metadata into an implementation-neutral filter description.
	 */
	FilterResult convert(FilterConversionContext context, T payload);
}
