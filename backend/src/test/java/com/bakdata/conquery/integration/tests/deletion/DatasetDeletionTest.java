package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.ws.rs.WebApplicationException;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
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

		final StandaloneSupport conquery = testConquery.getSupport(name);
		final MetaStorage storage = conquery.getMetaStorage();
		final Dataset dataset = conquery.getDataset();
		Namespace namespace = conquery.getNamespace();
		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();
		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, test.getContent().getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, test.getContent().getTables());
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
		final StandaloneSupport conqueryReimport = testConquery.getSupport(namespace.getDataset().getName());
		{
			// only import the deleted import/table
			LoadingUtil.importTables(conqueryReimport,test.getContent().getTables());

			assertThat(conqueryReimport.getNamespace().getStorage().getTables()).isNotEmpty();

			conqueryReimport.waitUntilWorkDone();
			LoadingUtil.importTableContents(conqueryReimport, test.getContent().getTables(), conqueryReimport.getDataset());

			conqueryReimport.waitUntilWorkDone();

			LoadingUtil.importConcepts(conqueryReimport, test.getRawConcepts());
			conqueryReimport.waitUntilWorkDone();

			assertThat(conqueryReimport.getDatasetsProcessor().getDatasetRegistry().get(conqueryReimport.getDataset().getId()))
					.describedAs("Dataset after re-import.")
					.isNotNull();

			assertThat(conqueryReimport.getNamespace().getStorage().getAllImports().size()).isEqualTo(nImports);

			for (ShardNode node : conqueryReimport.getShardNodes()) {
				assertThat(node.getWorkers().getWorkers().values())
						.filteredOn(w -> w.getInfo().getDataset().equals(conqueryReimport.getDataset().getId()))
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
			final StandaloneSupport conqueryRestart = testConquery.openDataset(conqueryReimport.getDataset().getId());

			log.info("Checking state after re-start");

			assertThat(conqueryRestart.getNamespace().getStorage().getAllImports().size()).isEqualTo(2);

			for (ShardNode node : conqueryRestart.getShardNodes()) {
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

			log.info("Executing query after restart");
			final Query query3 = IntegrationUtils.parseQuery(conqueryRestart, test.getRawQuery());

			// Issue a query and assert that it has the same content as the first time around.
			IntegrationUtils.assertQueryResult(conqueryRestart, query3, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}
	}
}
