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
				  		"name": "kh-diagnose",
				  		"table": "kh_diagnose"
				  	}
				  ],
				  "children":[
				    {
				      "name":"a00",
				      "label":"A00",
				      "condition":{"type":"AND","conditions":[{"type":"EQUAL","values":["A00","A000"]},{"type":"COLUMN_EQUAL","column":"aufnahmeart","values":["stationaer"]}]},
				      "children":[
				        {
				          "name":"a00_0",
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
		};

		List<DatasetMetadataFolderLoader.LoadedDatasetMetadata> loaded = loader.loadConfiguredDatasets();
		assertEquals(1, loaded.size());

		DatasetMetadataFolderLoader.LoadedDatasetMetadata dataset = loaded.getFirst();
		assertEquals("fdb_demo", dataset.dataset().id());
		assertEquals("demo", dataset.dataset().label());

		assertTrue(dataset.conceptsById().containsKey("fdb_demo.icd"));
		DatasetCatalogRepository.Concept concept = dataset.conceptsById().get("fdb_demo.icd");
		assertEquals("ICD", concept.label());
		assertEquals(List.of("fdb_demo.icd.a00"), concept.childrenIds());

		DatasetCatalogRepository.ConceptElement child = concept.children().get("fdb_demo.icd.a00");
		assertEquals("fdb_demo.icd", child.parentId());
		assertEquals("AND", child.condition().type());
		assertEquals(2, child.condition().conditions().size());
		assertEquals(List.of("A00", "A000"), child.condition().connectorValues());
		DatasetCatalogRepository.ConceptCondition columnCondition = child.condition().conditions().get(1);
		assertEquals("COLUMN_EQUAL", columnCondition.type());
		assertEquals("aufnahmeart", columnCondition.column());
		assertEquals(List.of("stationaer"), columnCondition.values());
		assertEquals(List.of("fdb_demo.icd.a00.a00_0"), child.children());

		DatasetCatalogRepository.ConceptElement leaf = concept.children().get("fdb_demo.icd.a00.a00_0");
		assertEquals("fdb_demo.icd.a00", leaf.parentId());
		assertEquals("EQUAL", leaf.condition().type());
		assertEquals(List.of("A000"), leaf.condition().connectorValues());

		assertTrue(dataset.tablesById().containsKey("fdb_demo.kh_diagnose"));
		DatasetCatalogRepository.TableRecord table = dataset.tablesById().get("fdb_demo.kh_diagnose");
		assertEquals("fdb_demo.kh_diagnose.icd_code", table.columns().get(0).id());
		assertEquals("icd_code", table.columns().get(0).secondaryId());
		assertEquals("fdb_demo.kh_diagnose.entlassungsdatum", table.columns().get(1).id());
		assertNotNull(table.columns().get(1).type());
	}

	@Test
	void loadsDevConfigMetadataFixtures() throws Exception {
		Path root = Path.of(Objects.requireNonNull(getClass().getResource("/test-meta-data")).toURI());

		DatasetMetadataFolderLoader loader = new DatasetMetadataFolderLoader();
		loader.objectMapper = new ObjectMapper();
		loader.validator = validator;
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
				return Optional.of(List.of("imdb", "empty-set"));
			}
		};

		List<DatasetMetadataFolderLoader.LoadedDatasetMetadata> loaded = loader.loadConfiguredDatasets();
		assertEquals(2, loaded.size());

		DatasetMetadataFolderLoader.LoadedDatasetMetadata imdb = loaded.stream()
																	  .filter(metadata -> metadata.dataset().id().equals("imdb"))
																	  .findFirst()
																	  .orElseThrow();
		assertEquals("imdb", imdb.dataset().id());
		assertEquals("IMDb", imdb.dataset().label());
		assertTrue(imdb.conceptsById().containsKey("imdb"));
		assertTrue(imdb.tablesById().containsKey("imdb.title"));

		DatasetCatalogRepository.TableRecord title = imdb.tablesById().get("imdb.title");
		assertEquals("Titles", title.label());
		assertEquals("imdb.title.id", title.primaryColumn());
		assertEquals(3, title.columns().size());
		assertEquals("Title ID", title.columns().get(0).label());
		assertEquals(DatasetCatalogRepository.ColumnType.INTEGER, title.columns().get(0).type());
		assertEquals("pid", title.columns().get(0).secondaryId());
		assertEquals("Title", title.columns().get(1).label());
		assertEquals(DatasetCatalogRepository.ColumnType.STRING, title.columns().get(1).type());
		assertEquals("Release Date", title.columns().get(2).label());
		assertEquals(DatasetCatalogRepository.ColumnType.DATE, title.columns().get(2).type());

		DatasetMetadataFolderLoader.LoadedDatasetMetadata emptySet = loaded.stream()
																		   .filter(metadata -> metadata.dataset().id().equals("empty-set"))
																		   .findFirst()
																		   .orElseThrow();
		assertTrue(emptySet.conceptsById().isEmpty());
		assertTrue(emptySet.tablesById().isEmpty());
	}

	@Test
	void fallsBackToFolderNameWhenDatasetJsonIsMissing(@TempDir Path tempDir) throws Exception {
		Path root = tempDir.resolve("test-datasets");
		String datasetName = "fallback-dataset-name";
		Path fallback = root.resolve(datasetName);
		Files.createDirectories(fallback.resolve("conceptTrees"));
		Files.createDirectories(fallback.resolve("tables"));

		DatasetMetadataFolderLoader loader = new DatasetMetadataFolderLoader();
		loader.objectMapper = new ObjectMapper();
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
		};

		DatasetCatalogRepository.DatasetRecord dataset = loader.loadConfiguredDatasets().getFirst().dataset();
		assertEquals(datasetName, dataset.id());
		assertEquals(datasetName, dataset.label());
	}
}
