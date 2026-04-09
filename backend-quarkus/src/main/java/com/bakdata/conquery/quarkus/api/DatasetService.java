package com.bakdata.conquery.quarkus.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class DatasetService {

	@Inject
	DatasetCatalogRepository datasetCatalog;

	public List<DatasetCatalogRepository.DatasetRecord> listDatasets() {
		return datasetCatalog.listDatasets();
	}

	public DatasetCatalogRepository.DatasetRecord requireDataset(String datasetId) {
		return datasetCatalog.findDataset(datasetId)
							 .orElseThrow(() -> new NotFoundException("Unknown dataset: " + datasetId));
	}

	public DatasetCatalogRepository.ConceptRecord requireConcept(String conceptId) {
		return datasetCatalog.findConcept(conceptId)
							 .orElseThrow(() -> new NotFoundException("Unknown concept: " + conceptId));
	}

	public List<DatasetCatalogRepository.ConceptRecord> listConceptsForDataset(String datasetId) {
		return datasetCatalog.listConcepts().stream().filter(concept -> concept.datasetId().equals(datasetId))
							 .toList();
	}

	public ConceptCodeResolution resolveConceptCodes(String rootConceptId, List<String> codes) {
		DatasetCatalogRepository.ConceptRecord rootConcept = requireConcept(rootConceptId);

		Map<String, String> lookupByCode = datasetCatalog.listConcepts().stream()
													.filter(concept -> concept.datasetId().equals(rootConcept.datasetId()))
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
