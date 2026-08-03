package com.bakdata.conquery.quarkus.concepts.selects.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ConceptSelectDefinitionAssembler {

	@Inject
	ConceptSelectDefinitionRegistry registry;

	@Inject
	ObjectMapper objectMapper;

	public List<DatasetCatalogRepository.ConceptSelect> assemble(
			ConceptId conceptId,
			List<ConceptSelectDefinition> definitions,
			ConceptSelectConversionContext.FallbackReporter fallbackReporter,
			boolean strictSelectTypes
	) {
		if (definitions == null) {
			return List.of();
		}
		ConceptSelectConversionContext context = new ConceptSelectConversionContext(conceptId, fallbackReporter);
		List<DatasetCatalogRepository.ConceptSelect> selects = new ArrayList<>();
		for (ConceptSelectDefinition definition : definitions) {
			assemble(context, definition, strictSelectTypes).ifPresent(selects::add);
		}
		return List.copyOf(selects);
	}

	private Optional<DatasetCatalogRepository.ConceptSelect> assemble(ConceptSelectConversionContext context, ConceptSelectDefinition definition, boolean strictSelectTypes) {
		Optional<ConceptSelectDefinitionProvider<?>> provider = registry.find(definition);
		if (provider.isPresent()) {
			return Optional.of(convert(context, definition, provider.get()));
		}
		String type = definition.getType();
		String reason = type == null || type.isBlank() ? "missing concept select type" : "unknown concept select type '" + type + "'";
		String message = "Skipping select for concept '" + context.conceptId() + "' because of " + reason + ": " + objectMapper.valueToTree(definition);
		if (strictSelectTypes) {
			throw new IllegalStateException(message);
		}
		log.warn("{}", message);
		return Optional.empty();
	}

	private <T extends ConceptSelectDefinition> DatasetCatalogRepository.ConceptSelect convert(ConceptSelectConversionContext context, ConceptSelectDefinition definition, ConceptSelectDefinitionProvider<T> provider) {
		return provider.convert(context, provider.payloadType().cast(definition));
	}
}
