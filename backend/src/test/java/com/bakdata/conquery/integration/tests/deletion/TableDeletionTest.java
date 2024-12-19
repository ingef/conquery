package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
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

		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final Namespace namespace = conquery.getNamespace();

		final TableId tableId = TableId.Parser.INSTANCE.parse(dataset.getName(), "test_table2");

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
		try(Stream<Import> allImports = namespace.getStorage().getAllImports()) {
			 nImports = allImports.count();
		}


		// State before deletion.
		{
			log.info("Checking state before deletion");
			// Must contain the import.
			assertThat(namespace.getStorage().getTable(tableId))
					.isNotNull();

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

			try(Stream<Concept<?>> allConcepts = conquery.getNamespace().getStorage().getAllConcepts()) {
				conquery.getAdminDatasetsProcessor().deleteConcept(allConcepts.iterator().next().getId());

			}

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
			try(Stream<Import> allImports = namespace.getStorage().getAllImports()) {
				assertThat(allImports.count()).isEqualTo(nImports - 1);
			}

			// The deleted import should not be found.
			try(Stream<Import> allImports = namespace.getStorage().getAllImports()) {
				assertThat(allImports)
						.filteredOn(imp -> imp.getId().getTable().equals(tableId))
						.isEmpty();
			}

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

					// No bucket should be found referencing the import.
					try(Stream<Bucket> allBuckets = workerStorage.getAllBuckets()) {

						assertThat(allBuckets)
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.filteredOn(bucket -> bucket.getImp().getTable().equals(tableId))
								.isEmpty();
					}

					// No CBlock associated with import may exist
					try(Stream<CBlock> allCBlocks = workerStorage.getAllCBlocks()) {
						assertThat(allCBlocks)
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.filteredOn(cBlock -> cBlock.getBucket().getImp().getTable().equals(tableId))
								.isEmpty();
					}

				}
			}

			log.info("Executing query after deletion. Expecting a failure here.");

			// Issue a query and assert that it has less content.
			IntegrationUtils.assertQueryResult(conquery, query, 0L, ExecutionState.FAILED, conquery.getTestUser(), 400);
		}

		conquery.waitUntilWorkDone();

		// Load the same import into the same table, with only the deleted import/table
		{
			// only import the deleted import/table
			LoadingUtil.importTables(conquery, content.getTables().stream()
													  .filter(table -> table.getName().equalsIgnoreCase(tableId.getTable()))
													  .collect(Collectors.toList()), content.isAutoConcept());

			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, content.getTables().stream()
															 .filter(table -> table.getName().equalsIgnoreCase(tableId.getTable()))
															 .collect(Collectors.toList()));
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

					assertThat(value.getStorage().getTable(tableId))
							.describedAs("Table in worker storage.")
							.isNotNull();
				}
			}
		}

		// Test state after reimport.
		{
			log.info("Checking state after re-import");
			try(Stream<Import> allImports = namespace.getStorage().getAllImports()) {
				assertThat(allImports.count()).isEqualTo(nImports);
			}

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

					try(Stream<Bucket> allBuckets = workerStorage.getAllBuckets()) {
						assertThat(allBuckets.filter(bucket -> bucket.getImp().getTable().equals(tableId)))
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.isNotEmpty();
					}
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

			StandaloneSupport conquery2 = testConquery.openDataset(dataset.getId());

			log.info("Checking state after re-start");

			{
				Namespace namespace2 = conquery2.getNamespace();
				try(Stream<Import> allImports = namespace2.getStorage().getAllImports()) {
					assertThat(allImports.count()).isEqualTo(2);
				}

				for (ShardNode node : conquery2.getShardNodes()) {
					for (Worker value : node.getWorkers().getWorkers().values()) {
						if (!value.getInfo().getDataset().equals(dataset.getId())) {
							continue;
						}

						final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

						try(Stream<Bucket> allBuckets = workerStorage.getAllBuckets()) {
							assertThat(allBuckets.filter(bucket -> bucket.getImp().getTable().equals(tableId)))
									.describedAs("Buckets for Worker %s", value.getInfo().getId())
									.isNotEmpty();
						}

					}
				}

				log.info("Executing query after re-import and restart");

				// Issue a query and assert that it has the same content as the first time around.
				IntegrationUtils.assertQueryResult(conquery2, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
			}
		}
	}
}
