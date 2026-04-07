package com.bakdata.conquery.quarkus.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.quarkus.api.config.ConceptsRuntimeConfig;
import com.bakdata.conquery.quarkus.api.config.DatasetsRuntimeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class DatasetService {

	@Inject
	DatasetsRuntimeConfig datasetsConfig;

	@Inject
	ConceptsRuntimeConfig conceptsConfig;

	public List<DatasetsRuntimeConfig.DatasetEntry> listDatasets() {
		return datasetsConfig.datasets();
	}

	public DatasetsRuntimeConfig.DatasetEntry requireDataset(String datasetId) {
		return datasetsConfig.datasets()
							 .stream()
							 .filter(dataset -> dataset.id().equals(datasetId))
							 .findFirst()
							 .orElseThrow(() -> new NotFoundException("Unknown dataset: " + datasetId));
	}

	public ConceptsRuntimeConfig.ConceptEntry requireConcept(String conceptId) {
		return conceptsConfig.concepts()
							 .stream()
							 .filter(concept -> concept.id().equals(conceptId))
							 .findFirst()
							 .orElseThrow(() -> new NotFoundException("Unknown concept: " + conceptId));
	}

	public List<ConceptsRuntimeConfig.ConceptEntry> listConceptsForDataset(String datasetId) {
		return conceptsConfig.concepts()
							 .stream()
							 .filter(concept -> concept.dataset().equals(datasetId))
							 .toList();
	}

	public ConceptCodeResolution resolveConceptCodes(String rootConceptId, List<String> codes) {
		ConceptsRuntimeConfig.ConceptEntry rootConcept = requireConcept(rootConceptId);

		Map<String, String> lookupByCode = conceptsConfig.concepts().stream()
													.filter(concept -> concept.dataset().equals(rootConcept.dataset()))
													.flatMap(concept -> java.util.stream.Stream.of(
															Map.entry(normalizeCode(concept.id()), concept.id()),
															Map.entry(normalizeCode(concept.label()), concept.id())
													))
													.collect(Collectors.toMap(
															Map.Entry::getKey,
															Map.Entry::getValue,
															(existing, ignored) -> existing
													));

		List<String> resolvedInOrder = codes.stream()
										.map(this::normalizeCode)
										.map(lookupByCode::get)
										.filter(java.util.Objects::nonNull)
										.toList();
		List<String> unknownCodes = codes.stream()
									 .filter(code -> !lookupByCode.containsKey(normalizeCode(code)))
									 .toList();

		return new ConceptCodeResolution(List.copyOf(new LinkedHashSet<>(resolvedInOrder)), unknownCodes);
	}

	private String normalizeCode(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	public record ConceptCodeResolution(
			List<String> resolvedConcepts,
			List<String> unknownCodes
	) {
	}
}
