package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminTablesResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

/**
 * Test if Imports can be deleted and safely queried.
 */
@Slf4j
public class ImportDeletionTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {


		final StandaloneSupport conquery = testConquery.getSupport(name);
		MetaStorage storage = conquery.getMetaStorage();

		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		final DatasetId dataset = conquery.getDataset();
		final Namespace namespace = conquery.getNamespace();

		final ImportId importId = ImportId.Parser.INSTANCE.parse(dataset.getName(), "test_table2", "test_table2");

		final QueryTest test = JsonIntegrationTest.readJson(dataset, testJson);

		// Manually import data, so we can do our own work.
		final RequiredData content = test.getContent();
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, content.getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, content.getTables(), content.isAutoConcept());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, content.getTables());
			conquery.waitUntilWorkDone();
		}

		final Query query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());

		final long nImports;
		try (Stream<ImportId> allImports = namespace.getStorage().getAllImports()) {
			nImports = allImports.count();
		}


		// State before deletion.
		{
			log.info("Checking state before deletion");

			// Must contain the import.
			try (Stream<ImportId> allImports = namespace.getStorage().getAllImports()) {
				assertThat(allImports)
						.filteredOn(imp -> imp.equals(importId))
						.isNotEmpty();
			}

			assertThat(namespace.getStorage().getImport(importId))
					.isNotNull();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker worker : node.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().equals(dataset)) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", worker.getInfo().getId())
							.filteredOn(block -> block.getBucket().getDataset().equals(dataset))
							.isNotEmpty();
					assertThat(IntegrationUtils.getAllBuckets(workerStorage))
							.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset))
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.isNotEmpty();

					// Must contain the import.
					assertThat(workerStorage.getImport(importId))
							.isNotNull();
				}
			}

			log.info("Executing query before deletion");

			IntegrationUtils.assertQueryResult(conquery, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}

		// Delete the import.
		{
			log.info("Issuing deletion of import {}", importId);

			final URI deleteImportUri =
					HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminTablesResource.class, "deleteImport")
								   .buildFromMap(Map.of(
										   ResourceConstants.DATASET, conquery.getDataset(),
										   ResourceConstants.TABLE, importId.getTable(),
										   ResourceConstants.IMPORT_ID, importId
								   ));

			final Response delete = conquery.getClient().target(deleteImportUri).request(MediaType.APPLICATION_JSON).delete();

			assertThat(delete.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);

			conquery.waitUntilWorkDone();

		}

		// State after deletion.
		{
			log.info("Checking state after deletion");
			// We have deleted an import now there should be one less!
			try (Stream<ImportId> allImports = namespace.getStorage().getAllImports()) {
				List<ImportId> imports = allImports.toList();
				assertThat(imports.size()).isEqualTo(nImports - 1);

				// The deleted import should not be found.
				assertThat(imports)
						.filteredOn(imp -> imp.equals(importId))
						.isEmpty();
			}

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker worker : node.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().equals(dataset)) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

					// No bucket should be found referencing the import.
					assertThat(IntegrationUtils.getAllBuckets(workerStorage))
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.filteredOn(bucket -> bucket.getImp().equals(importId))
							.isEmpty();

					// No CBlock associated with import may exist
					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", worker.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getBucket().getImp().equals(importId))
							.isEmpty();

					// Import should not exists anymore
					assertThat(workerStorage.getImport(importId))
							.describedAs("Import for Worker %s", worker.getInfo().getId())
							.isNull();
				}
			}

			log.info("Executing query after deletion");

			// Issue a query and assert that it has less content.
			IntegrationUtils.assertQueryResult(conquery, query, 1L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}

		conquery.waitUntilWorkDone();


		// Load more data under the same name into the same table, with only the deleted import/table
		{
			// only import the deleted import/table
			final RequiredTable import2Table = content.getTables().stream()
													  .filter(table -> table.getName().equalsIgnoreCase(importId.getTable().getTable()))
													  .findFirst()
													  .orElseThrow();


			final ResourceFile csv = import2Table.getCsv();
			final String path = csv.getPath();

			//copy csv to tmp folder
			// Content 2.2 contains an extra entry of a value that hasn't been seen before.
			FileUtils.copyInputStreamToFile(In.resource(path.substring(0, path.lastIndexOf('/')) + "/" + "content2.2.csv")
											  .asStream(), new File(conquery.getTmpDir(), csv.getName()));

			File descriptionFile = new File(conquery.getTmpDir(), import2Table.getName() + ConqueryConstants.EXTENSION_DESCRIPTION);
			File preprocessedFile =  new File(conquery.getTmpDir(), import2Table.getName() + ConqueryConstants.EXTENSION_PREPROCESSED);

			//create import descriptor

			TableImportDescriptor desc = new TableImportDescriptor();
			desc.setName(import2Table.getName());
			desc.setTable(import2Table.getName());
			TableInputDescriptor input = new TableInputDescriptor();
			{
				input.setPrimary(import2Table.getPrimaryColumn().createOutput());
				input.setSourceFile(import2Table.getCsv().getName());
				input.setOutput(new OutputDescription[import2Table.getColumns().length]);
				for (int i = 0; i < import2Table.getColumns().length; i++) {
					input.getOutput()[i] = import2Table.getColumns()[i].createOutput();
				}
			}
			desc.setInputs(new TableInputDescriptor[]{input});
			Jackson.MAPPER.writeValue(descriptionFile, desc);

			//preprocess
			conquery.preprocessTmp(conquery.getTmpDir(), List.of(descriptionFile));

			//import preprocessedFiles
			conquery.getAdminDatasetsProcessor().addImport(conquery.getNamespace(), new GZIPInputStream(new FileInputStream(preprocessedFile)));
			conquery.waitUntilWorkDone();
		}

		// State after reimport.
		{
			log.info("Checking state after re-import");

			try (Stream<ImportId> allImports = namespace.getStorage().getAllImports()) {
				assertThat(allImports.count()).isEqualTo(nImports);
			}

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker worker : node.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().equals(dataset)) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

					assertThat(IntegrationUtils.getAllBuckets(workerStorage))
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.filteredOn(bucket -> bucket.getImp().equals(importId))
							.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset))
							.isNotEmpty();
				}
			}

			log.info("Executing query after re-import");

			// Issue a query and assert that it has the same content as the first time around.
			IntegrationUtils.assertQueryResult(conquery, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}

		// Finally, restart conquery and assert again, that the data is correct.
		{
			testConquery.shutdown();

			//restart
			testConquery.beforeAll();

			StandaloneSupport conquery2 = testConquery.openDataset(dataset);
			log.info("Checking state after re-start");

			{
				try (Stream<ImportId> allImports = conquery2.getNamespace().getStorage().getAllImports()) {
					assertThat(allImports.count()).isEqualTo(2);
				}

				for (ShardNode node : conquery2.getShardNodes()) {
					for (Worker worker : node.getWorkers().getWorkers().values()) {

						if (!worker.getInfo().getDataset().equals(dataset))
							continue;

						final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

						try (Stream<Bucket> allBuckets = IntegrationUtils.getAllBuckets(workerStorage)) {
							assertThat(allBuckets)
									.describedAs("Buckets for Worker %s", worker.getInfo().getId())
									.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset))
									.filteredOn(bucket -> bucket.getImp().equals(importId))
									.isNotEmpty();
						}
					}
				}

				log.info("Executing query after re-import");

				// Issue a query and assert that it has the same content as the first time around.
				IntegrationUtils.assertQueryResult(conquery2, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
			}
		}
	}

}
