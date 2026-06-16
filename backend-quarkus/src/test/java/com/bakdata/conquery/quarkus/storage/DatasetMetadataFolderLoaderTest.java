package com.bakdata.conquery.quarkus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.api.config.DatasetMetadataRuntimeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatasetMetadataFolderLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void loadsConfiguredMetadataFolderAndScopesIds() throws Exception {
		Path root = tempDir.resolve("gen");
		Path demo = root.resolve("demo");
		Files.createDirectories(demo.resolve("conceptTrees"));
		Files.createDirectories(demo.resolve("tables"));

		Files.writeString(
				demo.resolve("structure_demo.json"),
				"""
				[
				  {"dataset":"fdb_demo","label":"Group 1"},
				  {"dataset":"fdb_demo","label":"Group 2"}
				]
				"""
		);
		Files.writeString(
				demo.resolve("conceptTrees/icd.concept.json"),
				"""
				{"name":"icd","label":"ICD"}
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
		DatasetCatalogRepository.ConceptRecord concept = dataset.conceptsById().get("fdb_demo.icd");
		assertEquals("ICD", concept.label());

		assertTrue(dataset.tablesById().containsKey("fdb_demo.kh_diagnose"));
		DatasetCatalogRepository.TableRecord table = dataset.tablesById().get("fdb_demo.kh_diagnose");
		assertEquals("fdb_demo.kh_diagnose.icd_code", table.columns().get(0).id());
		assertEquals("icd_code", table.columns().get(0).secondaryId());
		assertEquals("fdb_demo.kh_diagnose.entlassungsdatum", table.columns().get(1).id());
		assertNotNull(table.columns().get(1).type());
	}
}
