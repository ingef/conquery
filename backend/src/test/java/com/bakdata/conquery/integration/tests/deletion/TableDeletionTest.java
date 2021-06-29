package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

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
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminTablesResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

/**
 * Test if Imports can be deleted and safely queried.
 */
@Slf4j
public class TableDeletionTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);

		final MetaStorage storage = conquery.getMetaStorage();

		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final Namespace namespace = storage.getDatasetRegistry().get(dataset.getId());

		final TableId tableId = TableId.Parser.INSTANCE.parse(dataset.getName(), "test_table2");

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


		// State before deletion.
		{
			log.info("Checking state before deletion");
			// Must contain the import.
			assertThat(namespace.getStorage().getCentralRegistry().getOptional(tableId))
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

		// Delete the import.
		{
			log.info("Issuing deletion of import {}", tableId);

			// Delete the import via API.
			// But, we do not allow deletion of tables with associated connectors, so this should throw!

			final URI deleteTable =
					HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminTablesResource.class, "remove")
					.buildFromMap(Map.of(
							ResourceConstants.DATASET, conquery.getDataset().getName(),
							ResourceConstants.TABLE, tableId.toString()
					));

			final Response failed = conquery.getClient()
											.target(deleteTable)
											.request()
											.delete();

			assertThat(failed.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.CLIENT_ERROR);

			conquery.getDatasetsProcessor().deleteConcept(conquery.getNamespace().getStorage().getAllConcepts().iterator().next());

			Thread.sleep(100);
			conquery.waitUntilWorkDone();

			final Response success = conquery.getClient().target(deleteTable).request().delete();


			assertThat(success.getStatusInfo().getStatusCode()).isEqualTo(Response.Status.OK.getStatusCode());


			Thread.sleep(100);
			conquery.waitUntilWorkDone();
		}


		// State after deletion.
		{
			log.info("Checking state after deletion");
			// We have deleted an import now there should be two less!
			assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(nImports - 1);

			// The deleted import should not be found.
			assertThat(namespace.getStorage().getAllImports())
					.filteredOn(imp -> imp.getId().getTable().equals(tableId))
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
							.filteredOn(bucket -> bucket.getImp().getTable().getId().equals(tableId))
							.isEmpty();

					// No CBlock associated with import may exist
					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getBucket().getImp().getTable().getId().equals(tableId))
							.isEmpty();
				}
			}

			log.info("Executing query after deletion");

			// Issue a query and asseert that it has less content.
			IntegrationUtils.assertQueryResult(conquery, query, 0L, ExecutionState.FAILED, conquery.getTestUser(), 400);
		}

		conquery.waitUntilWorkDone();

		// Load the same import into the same table, with only the deleted import/table
		{
			// only import the deleted import/table
			conquery.getDatasetsProcessor().addTable(test.getContent().getTables().stream()
														 .filter(table -> table.getName().equalsIgnoreCase(tableId.getTable()))
														 .map(requiredTable -> requiredTable.toTable(conquery.getDataset(), conquery.getNamespace().getStorage().getCentralRegistry())).findFirst().get(), conquery.getNamespace());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, test.getContent().getTables().stream()
																 .filter(table -> table.getName().equalsIgnoreCase(tableId.getTable()))
																 .collect(Collectors.toList()), conquery.getDataset());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			assertThat(namespace.getStorage().getTable(tableId))
					.describedAs("Table after re-import.")
					.isNotNull();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					assertThat(value.getStorage().getCentralRegistry().resolve(tableId))
							.describedAs("Table in worker storage.")
							.isNotNull();
				}
			}
		}

		// Test state after reimport.
		{
			log.info("Checking state after re-import");
			assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(nImports);

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getAllBuckets().stream().filter(bucket -> bucket.getImp().getTable().getId().equals(tableId)))
							.describedAs("Buckets for Worker %s", value.getInfo().getId())
							.isNotEmpty();
				}
			}

			log.info("Executing query after re-import");

			// Issue a query and assert that it has the same content as the first time around.
			IntegrationUtils.assertQueryResult(conquery, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
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

			{
				assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(2);

				for (ShardNode node : conquery2.getShardNodes()) {
					for (Worker value : node.getWorkers().getWorkers().values()) {
						if (!value.getInfo().getDataset().equals(dataset.getId())) {
							continue;
						}

						final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllBuckets().stream().filter(bucket -> bucket.getImp().getTable().getId().equals(tableId)))
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
}
