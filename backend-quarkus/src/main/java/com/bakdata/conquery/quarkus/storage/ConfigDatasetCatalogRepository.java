package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.api.config.ConceptsRuntimeConfig;
import com.bakdata.conquery.quarkus.api.config.DatasetsRuntimeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigDatasetCatalogRepository implements DatasetCatalogRepository {

	@Inject
	DatasetsRuntimeConfig datasetsConfig;

	@Inject
	ConceptsRuntimeConfig conceptsConfig;

	@Override
	public List<DatasetRecord> listDatasets() {
		return datasetsConfig.datasets().stream()
							 .map(dataset -> new DatasetRecord(dataset.id(), dataset.label()))
							 .toList();
	}

	@Override
	public Optional<DatasetRecord> findDataset(String datasetId) {
		return listDatasets().stream().filter(dataset -> dataset.id().equals(datasetId)).findFirst();
	}

	@Override
	public List<ConceptRecord> listConcepts() {
		return conceptsConfig.concepts().stream()
							 .map(concept -> new ConceptRecord(concept.id(), concept.label(), concept.dataset()))
							 .toList();
	}

	@Override
	public Optional<ConceptRecord> findConcept(String conceptId) {
		return listConcepts().stream().filter(concept -> concept.id().equals(conceptId)).findFirst();
	}
}
