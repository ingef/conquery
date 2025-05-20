package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import jakarta.ws.rs.WebApplicationException;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

/**
 * Test if Imports can be deleted and safely queried.
 */
@Slf4j
public class DatasetDeletionTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);
		final DatasetId dataset = conquery.getDataset();
		Namespace namespace = conquery.getNamespace();
		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();
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

		log.info("Checking state before deletion");

		// Assert state before deletion.
		{
			// Must contain the import.
			assertThat(namespace.getStorage().getDataset())
					.isNotNull();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset)) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.isNotEmpty();
					assertThat(IntegrationUtils.getAllBuckets(workerStorage))
							.describedAs("Buckets for Worker %s", value.getInfo().getId())
							.isNotEmpty();
				}
			}

			log.info("Executing query before deletion");

			IntegrationUtils.assertQueryResult(conquery, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}

		// Delete Dataset.
		{
			log.info("Issuing deletion of dataset {}", dataset);

			// Delete the import.
			// But, we do not allow deletion of tables with associated connectors, so this should throw!
			assertThatThrownBy(() -> conquery.getAdminDatasetsProcessor().deleteDataset(dataset))
					.isInstanceOf(WebApplicationException.class);

			//TODO use api
			try (Stream<Table> tables = conquery.getNamespace().getStorage().getTables()) {
				tables.forEach(table -> conquery.getAdminDatasetsProcessor().deleteTable(table.getId(), true));
			}

			conquery.waitUntilWorkDone();

			// Finally delete dataset
			conquery.getAdminDatasetsProcessor().deleteDataset(dataset);

			conquery.waitUntilWorkDone();

			assertThat(conquery.getDatasetRegistry().get(dataset)).isNull();
		}

		// State after deletion.
		{
			log.info("Checking state after deletion");

			// We have deleted the dataset, there should be no imports
			assertThat(namespace.getStorage().getAllImports().count()).isEqualTo(0);

			// The deleted import should not be found.
			assertThat(namespace.getStorage().getAllImports())
					.filteredOn(imp -> imp.getTable().getDataset().equals(dataset))
					.isEmpty();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset)) {
						continue;
					}

//TODO					assertThat(datasetIdStream).as("No worker for the dataset %s should exist", dataset).doesNotContain(dataset);


					// Try to execute the query after deletion
					IntegrationUtils.assertQueryResult(conquery, query, 0, ExecutionState.FAILED, conquery.getTestUser(), 404);
				}
			}


			// Reload the dataset and assert the state.
			// We have to do some weird trix with StandaloneSupport to open it with another Dataset
			final StandaloneSupport conqueryReimport = testConquery.getSupport(dataset.getName());
			{
				// only import the deleted import/table
				LoadingUtil.importTables(conqueryReimport, content.getTables(), content.isAutoConcept());

				assertThat(conqueryReimport.getNamespace().getStorage().getTables()).isNotEmpty();

				conqueryReimport.waitUntilWorkDone();
				LoadingUtil.importTableContents(conqueryReimport, content.getTables());

				conqueryReimport.waitUntilWorkDone();

				LoadingUtil.importConcepts(conqueryReimport, test.getRawConcepts());
				conqueryReimport.waitUntilWorkDone();

				assertThat(conqueryReimport.getAdminDatasetsProcessor().getDatasetRegistry().get(conqueryReimport.getDataset()))
						.describedAs("Dataset after re-import.")
						.isNotNull();

				try (Stream<ImportId> allImports = conqueryReimport.getNamespace().getStorage().getAllImports()) {
					assertThat(allImports.count()).isEqualTo(nImports);
				}

				for (ShardNode node : conqueryReimport.getShardNodes()) {
					assertThat(node.getWorkers().getWorkers().values())
							.filteredOn(w -> w.getInfo().getDataset().equals(conqueryReimport.getDataset()))
							.describedAs("Workers for node {}", node.getName())
							.isNotEmpty();
				}

				log.info("Executing query after re-import");
				final Query query2 = IntegrationUtils.parseQuery(conqueryReimport, test.getRawQuery());

				// Issue a query and assert that it has the same content as the first time around.
				IntegrationUtils.assertQueryResult(conqueryReimport, query2, 2L, ExecutionState.DONE, conqueryReimport.getTestUser(), 201);
			}


			// Finally, restart conquery and assert again, that the data is correct.
			{
				testConquery.shutdown();

				//restart
				testConquery.beforeAll();
				final StandaloneSupport conqueryRestart = testConquery.openDataset(conqueryReimport.getDataset());

				log.info("Checking state after re-start");

				try (Stream<ImportId> allImports = conqueryRestart.getNamespace().getStorage().getAllImports()) {
					assertThat(allImports.count()).isEqualTo(2);
				}

				for (ShardNode node : conqueryRestart.getShardNodes()) {
					for (Worker value : node.getWorkers().getWorkers().values()) {
						if (!value.getInfo().getDataset().equals(dataset)) {
							continue;
						}

						final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

						assertThat(IntegrationUtils.getAllBuckets(workerStorage).filter(bucket -> bucket.getTable().getDataset().equals(dataset)))
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.isNotEmpty();
					}
				}

				log.info("Executing query after restart");
				final Query query3 = IntegrationUtils.parseQuery(conqueryRestart, test.getRawQuery());

				// Issue a query and assert that it has the same content as the first time around.
				IntegrationUtils.assertQueryResult(conqueryRestart, query3, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
			}
		}
	}
}

