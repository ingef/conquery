package com.bakdata.conquery.quarkus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.quarkus.config.DatasetMetadataRuntimeConfig;
import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinitionRegistry;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class DatasetMetadataFolderLoaderTest {

	@Inject
	Validator validator;

	@Inject
	FilterDefinitionRegistry filterDefinitionRegistry;

	@Test
	void loadsConfiguredMetadataFolderAndScopesIds(@TempDir Path tempDir) throws Exception {
		Path root = tempDir.resolve("gen");
		Path demo = root.resolve("demo");
		Files.createDirectories(demo.resolve("conceptTrees"));
		Files.createDirectories(demo.resolve("tables"));

		Files.writeString(
				demo.resolve("dataset.json"),
				"""
				{
				  "id":"fdb_demo"
				}
				"""
		);
		Files.writeString(
				demo.resolve("structure_demo.json"),
				"""
				[
				  {"label":"Group 1"},
				  {"label":"Group 2"}
				]
				"""
		);
		Files.writeString(
				demo.resolve("conceptTrees/icd.concept.json"),
				"""
				{
				  "name":"icd",
				  "label":"ICD",
				  "connectors": [
				  	{
				  		"label": "kh-diagnose",
				  		"name": "kh_diagnose",
				  		"table": "kh_diagnose",
				  		"filters": [
				  		  {
				  		    "type": "SELECT",
				  		    "label": "ICD Code",
				  		    "column": "icd_code",
				  		    "labels": {
				  		      "A00": "A00",
				  		      "A000": "A00.0"
				  		    }
				  		  }
				  		]
				  	}
				  ],
				  "children":[
				    {
				      "name":"a00",
				      "label":"A00",
				      "condition":{"type":"AND","conditions":[{"type":"EQUAL","values":["A00","A000"]},{"type":"COLUMN_EQUAL","column":"aufnahmeart","values":["stationaer"]}]},
				      "children":[
				        {
				          "label":"A00.0",
				          "condition":{"type":"EQUAL","values":["A000"]},
				          "children":[]
				        }
				      ]
				    }
				  ]
				}
				"""
		);
		Files.writeString(
				demo.resolve("tables/kh_diagnose.table.json"),
				"""
				{
				  "name":"kh_diagnose",
				  "columns":[
				    {"name":"icd_code","type":"STRING","secondaryId":"icd_code"},
				    {"name":"entlassungsdatum","type":"DATE"}
				  ]
				}
				"""
		);

		DatasetMetadataFolderLoader loader = new DatasetMetadataFolderLoader();
		loader.objectMapper = new ObjectMapper();
		loader.validator = validator;
		loader.filterDefinitionRegistry = filterDefinitionRegistry;
		loader.metadataConfig = new DatasetMetadataRuntimeConfig() {
			@Override
			public boolean enabled() {
				return true;
			}

			@Override
			public Optional<String> rootPath() {
				return Optional.of(root.toString());
			}

			@Override
			public Optional<List<String>> folders() {
				return Optional.of(List.of("demo"));
			}

			@Override
			public boolean strictFilterTypes() {
				return true;
			}
		};

		List<DatasetMetadataFolderLoader.LoadedDatasetMetadata> loaded = loader.loadConfiguredDatasets();
		assertEquals(1, loaded.size());

		DatasetMetadataFolderLoader.LoadedDatasetMetadata dataset = loaded.getFirst();
		assertEquals(DatasetId.parse("fdb_demo"), dataset.dataset().id());
		assertEquals("demo", dataset.dataset().label());

		assertTrue(dataset.conceptsById().containsKey(ConceptId.parse("fdb_demo.icd")));
		DatasetCatalogRepository.Concept concept = dataset.conceptsById().get(ConceptId.parse("fdb_demo.icd"));
		assertEquals("ICD", concept.label());
		assertEquals(List.of(ConceptId.parse("fdb_demo.icd.a00")), concept.childrenIds());
		DatasetCatalogRepository.Filter filter = concept.connectors().getFirst().filters().getFirst();
		assertEquals(FilterId.parse("fdb_demo.icd.kh_diagnose.ICD_Code"), filter.id());
		assertEquals("ICD Code", filter.label());
		assertEquals("MULTI_SELECT", filter.type());
		assertEquals(List.of(ColumnId.parse("fdb_demo.kh_diagnose.icd_code")), filter.requiredColumns());
		assertEquals(2, filter.options().size());

		DatasetCatalogRepository.ConceptElement child = concept.children().get(ConceptId.parse("fdb_demo.icd.a00"));
		assertEquals(ConceptId.parse("fdb_demo.icd"), child.parentId());
		assertEquals("AND", child.condition().type());
		assertEquals(2, child.condition().conditions().size());
		assertEquals(List.of("A00", "A000"), child.condition().connectorValues());
		DatasetCatalogRepository.ConceptCondition columnCondition = child.condition().conditions().get(1);
		assertEquals("COLUMN_EQUAL", columnCondition.type());
		assertEquals("aufnahmeart", columnCondition.column());
		assertEquals(List.of("stationaer"), columnCondition.values());
		assertEquals(List.of(ConceptId.parse("fdb_demo.icd.a00.A00_0")), child.children());

		DatasetCatalogRepository.ConceptElement leaf = concept.children().get(ConceptId.parse("fdb_demo.icd.a00.A00_0"));
		assertEquals(ConceptId.parse("fdb_demo.icd.a00"), leaf.parentId());
		assertEquals("EQUAL", leaf.condition().type());
		assertEquals(List.of("A000"), leaf.condition().connectorValues());

		assertTrue(dataset.tablesById().containsKey(TableId.parse("fdb_demo.kh_diagnose")));
		DatasetCatalogRepository.TableRecord table = dataset.tablesById().get(TableId.parse("fdb_demo.kh_diagnose"));
		assertEquals(ColumnId.parse("fdb_demo.kh_diagnose.icd_code"), table.columns().get(0).id());
		assertEquals("icd_code", table.columns().get(0).secondaryId());
		assertEquals(ColumnId.parse("fdb_demo.kh_diagnose.entlassungsdatum"), table.columns().get(1).id());
		assertNotNull(table.columns().get(1).type());
	}

	@Test
	void loadsDevConfigMetadataFixtures() throws Exception {
		Path root = Path.of(Objects.requireNonNull(getClass().getResource("/test-meta-data")).toURI());

		DatasetMetadataFolderLoader loader = new DatasetMetadataFolderLoader();
		loader.objectMapper = new ObjectMapper();
		loader.validator = validator;
		loader.filterDefinitionRegistry = filterDefinitionRegistry;
		loader.metadataConfig = new DatasetMetadataRuntimeConfig() {
			@Override
			public boolean enabled() {
				return true;
			}

			@Override
			public Optional<String> rootPath() {
				return Optional.of(root.toString());
			}

			@Override
			public Optional<List<String>> folders() {
				return Optional.of(List.of("imdb", "empty_set"));
			}

			@Override
			public boolean strictFilterTypes() {
				return true;
			}
		};

		List<DatasetMetadataFolderLoader.LoadedDatasetMetadata> loaded = loader.loadConfiguredDatasets();
		assertEquals(2, loaded.size());

		DatasetMetadataFolderLoader.LoadedDatasetMetadata imdb = loaded.stream()
																	  .filter(metadata -> metadata.dataset().id().equals(DatasetId.parse("imdb")))
																	  .findFirst()
																	  .orElseThrow();
		assertEquals(DatasetId.parse("imdb"), imdb.dataset().id());
		assertEquals("IMDb", imdb.dataset().label());
		assertTrue(imdb.conceptsById().containsKey(ConceptId.parse("imdb")));
		DatasetCatalogRepository.Filter titleFilter = imdb.conceptsById().get(ConceptId.parse("imdb")).connectors().getFirst().filters().getFirst();
		assertEquals(FilterId.parse("imdb.titles.release_year"), titleFilter.id());
		assertEquals("INTEGER_RANGE", titleFilter.type());
		assertEquals(List.of(ColumnId.parse("imdb.title.id")), titleFilter.requiredColumns());
		assertTrue(imdb.tablesById().containsKey(TableId.parse("imdb.title")));

		DatasetCatalogRepository.TableRecord title = imdb.tablesById().get(TableId.parse("imdb.title"));
		assertEquals("Titles", title.label());
		assertEquals(ColumnId.parse("imdb.title.id"), title.primaryColumn());
		assertEquals(3, title.columns().size());
		assertEquals("Title ID", title.columns().getFirst().label());
		assertEquals(DatasetCatalogRepository.ColumnType.INTEGER, title.columns().get(0).type());
		assertEquals("pid", title.columns().get(0).secondaryId());
		assertEquals("Title", title.columns().get(1).label());
		assertEquals(DatasetCatalogRepository.ColumnType.STRING, title.columns().get(1).type());
		assertEquals("Release Date", title.columns().get(2).label());
		assertEquals(DatasetCatalogRepository.ColumnType.DATE, title.columns().get(2).type());

		DatasetMetadataFolderLoader.LoadedDatasetMetadata emptySet = loaded.stream()
																		   .filter(metadata -> metadata.dataset().id().toString().equals("empty_set"))
																		   .findFirst()
																		   .orElseThrow();
		assertTrue(emptySet.conceptsById().isEmpty());
		assertTrue(emptySet.tablesById().isEmpty());
	}

	@Test
	void fallsBackToFolderNameWhenDatasetJsonIsMissing(@TempDir Path tempDir) throws Exception {
		Path root = tempDir.resolve("test-datasets");
		String datasetName = "fallback_dataset_name";
		Path fallback = root.resolve(datasetName);
		Files.createDirectories(fallback.resolve("conceptTrees"));
		Files.createDirectories(fallback.resolve("tables"));

		DatasetMetadataFolderLoader loader = new DatasetMetadataFolderLoader();
		loader.objectMapper = new ObjectMapper();
		loader.validator = validator;
		loader.filterDefinitionRegistry = filterDefinitionRegistry;
		loader.metadataConfig = new DatasetMetadataRuntimeConfig() {
			@Override
			public boolean enabled() {
				return true;
			}

			@Override
			public Optional<String> rootPath() {
				return Optional.of(root.toString());
			}

			@Override
			public Optional<List<String>> folders() {
				return Optional.of(List.of(datasetName));
			}

			@Override
			public boolean strictFilterTypes() {
				return true;
			}
		};

		DatasetCatalogRepository.DatasetRecord dataset = loader.loadConfiguredDatasets().getFirst().dataset();
		assertEquals(DatasetId.parse(datasetName), dataset.id());
		assertEquals(datasetName, dataset.label());
	}
}
