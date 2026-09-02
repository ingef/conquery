package com.bakdata.conquery.quarkus.concepts.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValueRegistry;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinitionProvider;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
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
			FilterFallbackReporter fallbackReporter,
			boolean strictFilterTypes
	) {
		if (definitions == null) {
			return List.of();
		}

		BackendFilterConversionContext context = new BackendFilterConversionContext(connectorId, tableId, table, fallbackReporter);
		List<DatasetCatalogRepository.Filter> filters = new ArrayList<>();
		for (FilterDefinition definition : definitions) {
			assemble(context, definition, strictFilterTypes).ifPresent(filters::add);
		}
		return List.copyOf(filters);
	}

	private Optional<DatasetCatalogRepository.Filter> assemble(BackendFilterConversionContext context, FilterDefinition definition, boolean strictFilterTypes) {
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

	private <T extends FilterDefinition> DatasetCatalogRepository.Filter convert(BackendFilterConversionContext context, FilterDefinition definition, FilterDefinitionProvider<T> provider) {
		T payload = provider.modelType().cast(definition);
		FilterResult result = provider.convert(context, payload);
		if (!provider.acceptedValueTypes().contains(result.valueType())) {
			throw new IllegalStateException("Filter provider " + provider.getClass().getName()
					+ " produced filter value type '" + result.valueType() + "' but accepts " + provider.acceptedValueTypes());
		}
		filterValueRegistry.require(result.valueType());
		List<ColumnId> requiredColumns = result.requiredColumns().stream()
				.map(context::requireColumn)
				.map(column -> context.columnId(column.name()))
				.toList();
		return new DatasetCatalogRepository.Filter(
				new FilterId(context.connectorId(), result.name()),
				definition,
				result.label(),
				result.valueType(),
				result.unit(),
				result.tooltip(),
				result.options().stream()
						.map(option -> new DatasetCatalogRepository.FrontendValue(option.value(), option.label(), option.optionValue()))
						.toList(),
				result.min(),
				result.max(),
				result.pattern(),
				result.allowDropFile(),
				result.creatable(),
				result.defaultValue(),
				requiredColumns
		);
	}
}
