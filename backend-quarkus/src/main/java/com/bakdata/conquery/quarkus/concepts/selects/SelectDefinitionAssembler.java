package com.bakdata.conquery.quarkus.concepts.selects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class SelectDefinitionAssembler {

	@Inject
	SelectDefinitionRegistry registry;

	@Inject
	ObjectMapper objectMapper;

	public List<DatasetCatalogRepository.Select> assemble(
			ConnectorId connectorId,
			TableId tableId,
			DatasetCatalogRepository.TableRecord table,
			List<SelectDefinition> definitions,
			SelectConversionContext.FallbackReporter fallbackReporter,
			boolean strictSelectTypes
	) {
		if (definitions == null) {
			return List.of();
		}
		SelectConversionContext context = new SelectConversionContext(connectorId, tableId, table, fallbackReporter);
		List<DatasetCatalogRepository.Select> selects = new ArrayList<>();
		for (SelectDefinition definition : definitions) {
			assemble(context, definition, strictSelectTypes).ifPresent(selects::add);
		}
		return List.copyOf(selects);
	}

	private Optional<DatasetCatalogRepository.Select> assemble(SelectConversionContext context, SelectDefinition definition, boolean strictSelectTypes) {
		Optional<SelectDefinitionProvider<?>> provider = registry.find(definition);
		if (provider.isPresent()) {
			return Optional.of(convert(context, definition, provider.get()));
		}
		String type = definition.getType();
		String reason = type == null || type.isBlank() ? "missing select type" : "unknown select type '" + type + "'";
		String message = "Skipping select for connector '" + context.connectorId() + "' because of " + reason + ": " + objectMapper.valueToTree(definition);
		if (strictSelectTypes) {
			throw new IllegalStateException(message);
		}
		log.warn("{}", message);
		return Optional.empty();
	}

	private <T extends SelectDefinition> DatasetCatalogRepository.Select convert(SelectConversionContext context, SelectDefinition definition, SelectDefinitionProvider<T> provider) {
		return provider.convert(context, provider.modelType().cast(definition));
	}
}
