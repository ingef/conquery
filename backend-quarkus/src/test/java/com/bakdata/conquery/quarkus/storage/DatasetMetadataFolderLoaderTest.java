package com.bakdata.conquery.quarkus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.quarkus.config.DatasetMetadataRuntimeConfig;
import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinitionAssembler;
import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinitionAssembler;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.ids.SelectId;
import com.bakdata.conquery.quarkus.ids.StructureNodeId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class DatasetMetadataFolderLoaderTest {

	@Inject
	Validator validator;

	@Inject
	FilterDefinitionAssembler filterDefinitionAssembler;

	@Inject
	SelectDefinitionAssembler selectDefinitionAssembler;

	@Inject
	ObjectMapper objectMapper;

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
				  {
				    "label":"Group 1",
				    "children":[{"name":"diagnoses","label":"Diagnoses","containedRoots":["icd"]}]
				  },
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
						"default": false,
				  		"filters": [
				  		  {
				  		    "type": "SELECT",
				  		    "label": "ICD Code",
				  		    "column": "icd_code",
				  		    "labels": {
				  		      "A00": "A00",
				  		      "A000": "A00.0"
				  		    }
				  		  },
				  		  {
				  		    "type": "COUNT",
				  		    "name": "icd_count",
				  		    "label": "ICD Count",
				  		    "column": "icd_code",
				  		    "distinctByColumn": "entlassungsdatum"
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
		loader.objectMapper = objectMapper;
		loader.validator = validator;
		loader.filterDefinitionAssembler = filterDefinitionAssembler;
		loader.selectDefinitionAssembler = selectDefinitionAssembler;
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

			@Override
			public boolean strictSelectTypes() {
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
		assertEquals(false, concept.connectors().getFirst().isDefault());
		assertEquals(3, dataset.structureNodesById().size());
		DatasetCatalogRepository.StructureNode diagnoses = dataset.structureNodesById().get(StructureNodeId.parse("fdb_demo.Group_1.diagnoses"));
		assertEquals(StructureNodeId.parse("fdb_demo.Group_1"), diagnoses.parentId());
		assertEquals(List.of(ConceptId.parse("fdb_demo.icd")), diagnoses.containedRoots());
		DatasetCatalogRepository.Filter filter = concept.connectors().getFirst().filters().getFirst();
		assertEquals(FilterId.parse("fdb_demo.icd.kh_diagnose.ICD_Code"), filter.id());
		assertEquals("ICD Code", filter.label());
		assertEquals("MULTI_SELECT", filter.type());
		assertEquals(List.of(ColumnId.parse("fdb_demo.kh_diagnose.icd_code")), filter.requiredColumns());
		assertEquals(2, filter.options().size());
		DatasetCatalogRepository.Filter countFilter = concept.connectors().getFirst().filters().get(1);
		assertEquals(FilterId.parse("fdb_demo.icd.kh_diagnose.icd_count"), countFilter.id());
		assertEquals(
				List.of(ColumnId.parse("fdb_demo.kh_diagnose.icd_code"), ColumnId.parse("fdb_demo.kh_diagnose.entlassungsdatum")),
				countFilter.requiredColumns()
		);

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
		loader.objectMapper = objectMapper;
		loader.validator = validator;
		loader.filterDefinitionAssembler = filterDefinitionAssembler;
		loader.selectDefinitionAssembler = selectDefinitionAssembler;
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

			@Override
			public boolean strictSelectTypes() {
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
		assertEquals(FilterId.parse("imdb.titles.release_age"), titleFilter.id());
		assertEquals("INTEGER_RANGE", titleFilter.type());
		assertEquals(List.of(ColumnId.parse("imdb.title.release_date")), titleFilter.requiredColumns());
		DatasetCatalogRepository.Select titleSelect = imdb.conceptsById().get(ConceptId.parse("imdb")).connectors().getFirst().selects().getFirst();
		assertEquals(SelectId.parse("imdb.titles.Title"), titleSelect.id());
		assertEquals("FIRST", titleSelect.implementationType());
		assertEquals(DatasetCatalogRepository.SelectResultType.primitive("STRING"), titleSelect.resultType());
		assertEquals(List.of(ColumnId.parse("imdb.title.name")), titleSelect.requiredColumns());
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
		loader.objectMapper = objectMapper;
		loader.validator = validator;
		loader.filterDefinitionAssembler = filterDefinitionAssembler;
		loader.selectDefinitionAssembler = selectDefinitionAssembler;
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

			@Override
			public boolean strictSelectTypes() {
				return true;
			}
		};

		DatasetCatalogRepository.DatasetRecord dataset = loader.loadConfiguredDatasets().getFirst().dataset();
		assertEquals(DatasetId.parse(datasetName), dataset.id());
		assertEquals(datasetName, dataset.label());
	}

	@Test
	void skipsUnknownFilterInLenientModeAndRejectsItInStrictMode(@TempDir Path tempDir) throws Exception {
		Path dataset = tempDir.resolve("demo");
		Files.createDirectories(dataset.resolve("conceptTrees"));
		Files.createDirectories(dataset.resolve("tables"));
		Files.writeString(dataset.resolve("tables/events.table.json"), """
				{"name":"events","columns":[{"name":"value","type":"STRING"}]}
				""");
		Files.writeString(dataset.resolve("conceptTrees/events.concept.json"), """
				{
				  "name":"events",
				  "children":[],
				  "connectors":[{
				    "name":"events",
				    "table":"events",
				    "filters":[{"type":"EXTERNAL_FILTER","column":"value"}]
				  }]
				}
				""");

		DatasetMetadataFolderLoader lenientLoader = loader(tempDir, false);
		DatasetCatalogRepository.Concept concept = lenientLoader.loadConfiguredDatasets().getFirst().conceptsById().get(ConceptId.parse("demo.events"));
		assertTrue(concept.connectors().getFirst().filters().isEmpty());

		DatasetMetadataFolderLoader strictLoader = loader(tempDir, true);
		IllegalStateException error = assertThrows(IllegalStateException.class, strictLoader::loadConfiguredDatasets);
		assertTrue(error.getMessage().contains("unknown filter type 'EXTERNAL_FILTER'"));
	}

	@Test
	void skipsUnknownSelectInLenientModeAndRejectsItInStrictMode(@TempDir Path tempDir) throws Exception {
		Path dataset = tempDir.resolve("demo");
		Files.createDirectories(dataset.resolve("conceptTrees"));
		Files.createDirectories(dataset.resolve("tables"));
		Files.writeString(dataset.resolve("tables/events.table.json"), """
				{"name":"events","columns":[{"name":"value","type":"STRING"}]}
				""");
		Files.writeString(dataset.resolve("conceptTrees/events.concept.json"), """
				{
				  "name":"events",
				  "children":[],
				  "connectors":[{
				    "name":"events",
				    "table":"events",
				    "selects":[{"type":"EXTERNAL_SELECT","column":"value"}]
				  }]
				}
				""");

		DatasetCatalogRepository.Concept concept = loader(tempDir, false).loadConfiguredDatasets().getFirst().conceptsById().get(ConceptId.parse("demo.events"));
		assertTrue(concept.connectors().getFirst().selects().isEmpty());

		IllegalStateException error = assertThrows(IllegalStateException.class, () -> loader(tempDir, true).loadConfiguredDatasets());
		assertTrue(error.getMessage().contains("unknown select type 'EXTERNAL_SELECT'"));
	}

	@Test
	void rejectsStructureNodeWithUnknownConceptAtStartup(@TempDir Path tempDir) throws Exception {
		Path dataset = tempDir.resolve("demo");
		Files.createDirectories(dataset.resolve("conceptTrees"));
		Files.createDirectories(dataset.resolve("tables"));
		Files.writeString(dataset.resolve("structure_demo.json"), """
				[{"name":"invalid","containedRoots":["missing"]}]
				""");

		IllegalStateException error = assertThrows(IllegalStateException.class, () -> loader(tempDir, true).loadConfiguredDatasets());
		assertTrue(error.getMessage().contains("Structure node 'demo.invalid' references unknown concept 'demo.missing'"));
	}

	@Test
	void reportsCascadedFilterConstraintWithFileAndPropertyPath(@TempDir Path tempDir) throws Exception {
		Path dataset = tempDir.resolve("demo");
		Files.createDirectories(dataset.resolve("conceptTrees"));
		Files.createDirectories(dataset.resolve("tables"));
		Files.writeString(dataset.resolve("tables/events.table.json"), """
				{"name":"events","columns":[{"name":"value","type":"INTEGER"}]}
				""");
		Path conceptFile = dataset.resolve("conceptTrees/events.concept.json");
		Files.writeString(conceptFile, """
				{
				  "name":"events",
				  "children":[],
				  "connectors":[{
				    "name":"events",
				    "table":"events",
				    "filters":[{"type":"NUMBER","name":"missing_column"}]
				  }]
				}
				""");

		ConstraintViolationException error = assertThrows(
				ConstraintViolationException.class,
				() -> loader(tempDir, true).loadConfiguredDatasets()
		);

		assertTrue(error.getMessage().contains(conceptFile.toString()));
		assertTrue(error.getMessage().contains("connectors[0].filters[0].column: must not be blank"));
		assertTrue(error.getMessage().contains("invalid value: null"));
	}

	private DatasetMetadataFolderLoader loader(Path root, boolean strictFilterTypes) {
		DatasetMetadataFolderLoader loader = new DatasetMetadataFolderLoader();
		loader.objectMapper = objectMapper;
		loader.validator = validator;
		loader.filterDefinitionAssembler = filterDefinitionAssembler;
		loader.selectDefinitionAssembler = selectDefinitionAssembler;
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
				return strictFilterTypes;
			}

			@Override
			public boolean strictSelectTypes() {
				return strictFilterTypes;
			}
		};
		return loader;
	}
}
