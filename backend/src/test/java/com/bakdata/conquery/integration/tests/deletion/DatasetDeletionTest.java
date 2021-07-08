package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.ws.rs.WebApplicationException;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

/**
 * Test if Imports can be deleted and safely queried.
 *
 */
@Slf4j
public class DatasetDeletionTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		StandaloneSupport conquery = testConquery.getSupport(name);
		final MetaStorage storage = conquery.getMetaStorage();
		final Dataset dataset = conquery.getDataset();
		Namespace namespace = storage.getDatasetRegistry().get(dataset.getId());
		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();
		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, test.getContent().getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, test.getContent());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, test.getContent().getTables(), conquery.getDataset());
			conquery.waitUntilWorkDone();
		}

		final Query query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());

		final int nImports = namespace.getStorage().getAllImports().size();

		log.info("Checking state before deletion");

		// Assert state before deletion.
		{
			// Must contain the import.
			assertThat(namespace.getStorage().getCentralRegistry().getOptional(dataset.getId()))
					.isNotEmpty();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.isNotEmpty();
					assertThat(workerStorage.getAllBuckets())
							.describedAs("Buckets for Worker %s", value.getInfo().getId())
							.isNotEmpty();
				}
			}

			log.info("Executing query before deletion");

			IntegrationUtils.assertQueryResult(conquery, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}

		// Delete Dataset.
		{
			log.info("Issuing deletion of import {}", dataset);

			// Delete the import.
			// But, we do not allow deletion of tables with associated connectors, so this should throw!
			assertThatThrownBy(() -> conquery.getDatasetsProcessor().deleteDataset(dataset))
					.isInstanceOf(WebApplicationException.class);

			//TODO use api
			conquery.getNamespace().getStorage().getTables()
					.forEach(tableId -> conquery.getDatasetsProcessor().deleteTable(tableId, true));

			conquery.waitUntilWorkDone();

			// Finally delete dataset
			conquery.getDatasetsProcessor().deleteDataset(dataset);

			conquery.waitUntilWorkDone();

			assertThat(storage.getCentralRegistry().getOptional(dataset.getId())).isEmpty();
		}

		// State after deletion.
		{
			log.info("Checking state after deletion");

			// We have deleted an import now there should be two less!
			assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(0);

			// The deleted import should not be found.
			assertThat(namespace.getStorage().getAllImports())
					.filteredOn(imp -> imp.getId().getTable().getDataset().equals(dataset.getId()))
					.isEmpty();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

					// No bucket should be found referencing the import.
					assertThat(workerStorage.getAllBuckets())
							.describedAs("Buckets for Worker %s", value.getInfo().getId())
							.filteredOn(bucket -> bucket.getTable().getDataset().getId().equals(dataset.getId()))
							.isEmpty();

					// No CBlock associated with import may exist
					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getBucket().getTable().getDataset().getId().equals(dataset.getId()))
							.isEmpty();
				}
			}


			// It's not exactly possible to issue a query for a non-existant dataset, so we assert that parsing the fails.
			assertThatThrownBy(() -> {
				IntegrationUtils.parseQuery(conquery, test.getRawQuery());
			}).isNotNull();

			IntegrationUtils.assertQueryResult(conquery, query, 0, ExecutionState.FAILED, conquery.getTestUser(), 404);
		}


		// Reload the dataset and assert the state.
		// We have to do some weird trix with StandaloneSupport to open it with another Dataset
		{
			final Dataset newDataset = conquery.getDatasetsProcessor().addDataset(dataset.getName());
			conquery.waitUntilWorkDone();

			final StandaloneSupport conquery2 =
					new StandaloneSupport(
							testConquery,
							storage.getDatasetRegistry()
								   .get(dataset.getId()),
							newDataset,
							conquery.getTmpDir(),
							conquery.getConfig(),
							conquery.getMetaProcessor(),
							conquery.getDatasetsProcessor(),
							conquery.getTestUser()
					);


			namespace = storage.getDatasetRegistry().get(dataset.getId());

			// only import the deleted import/table
			for (RequiredTable table : test.getContent().getTables()) {
				conquery2.getDatasetsProcessor().addTable(table.toTable(conquery.getDataset(), conquery2.getNamespace().getStorage().getCentralRegistry()), conquery2.getNamespace());
			}

			assertThat(conquery2.getNamespace().getStorage().getTables()).isNotEmpty();

			conquery.waitUntilWorkDone();
			LoadingUtil.importTableContents(conquery2, test.getContent().getTables(), newDataset);

			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery2, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			assertThat(conquery2.getDatasetsProcessor().getDatasetRegistry().get(dataset.getId()))
					.describedAs("Dataset after re-import.")
					.isNotNull();

			assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(nImports);

			for (ShardNode node : conquery.getShardNodes()) {
				assertThat(node.getWorkers().getWorkers().values())
						.filteredOn(w -> w.getInfo().getDataset().equals(dataset.getId()))
						.describedAs("Workers for node {}", node.getName())
						.isNotEmpty();
			}

			log.info("Executing query after re-import");

			// Issue a query and assert that it has the same content as the first time around.
			IntegrationUtils.assertQueryResult(conquery2, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}


		// Finally, restart conquery and assert again, that the data is correct.
		{
			testConquery.shutdown(conquery);
			//stop dropwizard directly so ConquerySupport does not delete the tmp directory
			testConquery.getDropwizard().after();
			//restart
			testConquery.beforeAll();
			StandaloneSupport conquery2 = testConquery.openDataset(dataset.getId());

			log.info("Checking state after re-start");

				assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(2);

				for (ShardNode node : conquery2.getShardNodes()) {
					for (Worker value : node.getWorkers().getWorkers().values()) {
						if (!value.getInfo().getDataset().equals(dataset.getId())) {
							continue;
						}

						final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllBuckets().stream().filter(bucket -> bucket.getTable().getDataset().getId().equals(dataset.getId())))
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.isNotEmpty();
					}
				}

				log.info("Executing query after re-import");

				// Issue a query and assert that it has the same content as the first time around.
				IntegrationUtils.assertQueryResult(conquery2, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}
	}
}
