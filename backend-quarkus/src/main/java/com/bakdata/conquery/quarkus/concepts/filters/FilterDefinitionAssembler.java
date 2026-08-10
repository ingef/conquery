package com.bakdata.conquery.quarkus.concepts.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValueProvider;
import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValueRegistry;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class FilterDefinitionAssembler {

	@Inject
	FilterDefinitionRegistry registry;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	FilterValueRegistry filterValueRegistry;

	public List<DatasetCatalogRepository.Filter> assemble(
			ConnectorId connectorId,
			TableId tableId,
			DatasetCatalogRepository.TableRecord table,
			List<FilterDefinition> definitions,
			FilterConversionContext.FallbackReporter fallbackReporter,
			boolean strictFilterTypes
	) {
		if (definitions == null) {
			return List.of();
		}

		FilterConversionContext context = new FilterConversionContext(connectorId, tableId, table, fallbackReporter);
		List<DatasetCatalogRepository.Filter> filters = new ArrayList<>();
		for (FilterDefinition definition : definitions) {
			assemble(context, definition, strictFilterTypes).ifPresent(filters::add);
		}
		return List.copyOf(filters);
	}

	private Optional<DatasetCatalogRepository.Filter> assemble(FilterConversionContext context, FilterDefinition definition, boolean strictFilterTypes) {
		Optional<FilterDefinitionProvider<?>> provider = registry.find(definition);
		if (provider.isPresent()) {
			return Optional.of(convert(context, definition, provider.get()));
		}

		String type = definition.getType();
		String reason = type == null || type.isBlank() ? "missing filter type" : "unknown filter type '" + type + "'";
		String message = "Skipping filter for connector '" + context.connectorId() + "' because of " + reason + ": " + objectMapper.valueToTree(definition);
		if (strictFilterTypes) {
			throw new IllegalStateException(message);
		}
		log.warn("{}", message);
		return Optional.empty();
	}

	private <T extends FilterDefinition> DatasetCatalogRepository.Filter convert(FilterConversionContext context, FilterDefinition definition, FilterDefinitionProvider<T> provider) {
		T payload = provider.modelType().cast(definition);
		DatasetCatalogRepository.Filter filter = provider.convert(context, payload);
		Set<String> acceptedTypes = provider.acceptedValueTypes().stream()
				.map(filterValueRegistry::require)
				.map(FilterValueProvider::type)
				.collect(Collectors.toUnmodifiableSet());
		if (!acceptedTypes.contains(filter.type())) {
			throw new IllegalStateException("Filter provider " + provider.getClass().getName()
					+ " produced filter value type '" + filter.type() + "' but accepts " + acceptedTypes);
		}
		return filter;
	}
}
