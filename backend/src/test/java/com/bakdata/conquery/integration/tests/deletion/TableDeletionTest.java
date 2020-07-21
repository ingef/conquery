package com.bakdata.conquery.integration.tests.deletion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.IQuery;
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
public class TableDeletionTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);

		final MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();

		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		final DatasetId dataset = conquery.getDataset().getId();
		final Namespace namespace = storage.getNamespaces().get(dataset);

		final TableId tableId = TableId.Parser.INSTANCE.parse(dataset.getName(), "test_table2");

		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);
		final IQuery query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			IntegrationUtils.importTables(conquery, test.getContent());
			conquery.waitUntilWorkDone();

			IntegrationUtils.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			IntegrationUtils.importTableContents(conquery, Arrays.asList(test.getContent().getTables()), conquery.getDataset());
			conquery.waitUntilWorkDone();
		}

		final int nImports = namespace.getStorage().getAllImports().size();


		// State before deletion.
		{
			log.info("Checking state before deletion");
			// Must contain the import.
			assertThat(namespace.getStorage().getCentralRegistry().getOptional(tableId))
					.isNotEmpty();

			for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
				for (Worker value : slave.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().getDataset().equals(dataset)) {
						continue;
					}

					final WorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.isNotEmpty();
					assertThat(workerStorage.getAllBuckets())
							.describedAs("Buckets for Worker %s", value.getInfo().getId())
							.isNotEmpty();
				}
			}

			log.info("Executing query before deletion");

			ConceptUpdateAndDeletionTest.assertQueryResult(conquery, query, 2L, ExecutionState.DONE);
		}

		// Delete the import.
		{
			log.info("Issuing deletion of import {}", tableId);

			// Delete the import.
			// But, we do not allow deletion of tables with associated connectors, so this should throw!
			assertThatThrownBy(() -> conquery.getDatasetsProcessor().deleteTable(tableId))
					.isInstanceOf(IllegalArgumentException.class);

			conquery.getDatasetsProcessor().deleteConcept(conquery.getNamespace().getStorage().getAllConcepts().iterator().next().getId());

			Thread.sleep(100);
			conquery.waitUntilWorkDone();

			conquery.getDatasetsProcessor().deleteTable(tableId);

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

			for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
				for (Worker value : slave.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().getDataset().equals(dataset)) {
						continue;
					}

					final WorkerStorage workerStorage = value.getStorage();

					// No bucket should be found referencing the import.
					assertThat(workerStorage.getAllBuckets())
							.describedAs("Buckets for Worker %s", value.getInfo().getId())
							.filteredOn(bucket -> bucket.getImp().getId().getTable().equals(tableId))
							.isEmpty();

					// No CBlock associated with import may exist
					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getBucket().getImp().getTable().equals(tableId))
							.isEmpty();
				}
			}

			log.info("Executing query after deletion");

			// Issue a query and asseert that it has less content.
			ConceptUpdateAndDeletionTest.assertQueryResult(conquery, query, 0L, ExecutionState.FAILED);
		}

		conquery.waitUntilWorkDone();

		// Load the same import into the same table, with only the deleted import/table
		{
			// only import the deleted import/table
			conquery.getDatasetsProcessor().addTable(namespace.getDataset(), Arrays.stream(test.getContent().getTables())
																				   .filter(table -> table.getName().equalsIgnoreCase(tableId.getTable()))
																				   .map(RequiredTable::toTable).findFirst().get());
			conquery.waitUntilWorkDone();

			IntegrationUtils.importTableContents(conquery, Arrays.stream(test.getContent().getTables())
																 .filter(table -> table.getName().equalsIgnoreCase(tableId.getTable()))
																 .collect(Collectors.toList()), conquery.getDataset());
			conquery.waitUntilWorkDone();

			IntegrationUtils.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			assertThat(namespace.getDataset().getTables().getOptional(tableId))
					.describedAs("Table after re-import.")
					.isPresent();

			for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
				for (Worker value : slave.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().getDataset().equals(dataset)) {
						continue;
					}

					final WorkerStorage workerStorage = value.getStorage();

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

			for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
				for (Worker value : slave.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().getDataset().equals(dataset)) {
						continue;
					}

					final WorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getAllBuckets().stream().filter(bucket -> bucket.getImp().getId().getTable().equals(tableId)))
							.describedAs("Buckets for Worker %s", value.getInfo().getId())
							.isNotEmpty();
				}
			}

			log.info("Executing query after re-import");

			// Issue a query and assert that it has the same content as the first time around.
			ConceptUpdateAndDeletionTest.assertQueryResult(conquery, query, 2L, ExecutionState.DONE);
		}

		// Finally, restart conquery and assert again, that the data is correct.
		{

			//stop dropwizard directly so ConquerySupport does not delete the tmp directory
			testConquery.getDropwizard().after();
			//restart
			testConquery.beforeAll(testConquery.getBeforeAllContext());

			StandaloneSupport conquery2 = testConquery.openDataset(dataset);

			log.info("Checking state after re-start");

			{
				assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(2);

				for (SlaveCommand slave : conquery2.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						if (!value.getInfo().getDataset().getDataset().equals(dataset)) {
							continue;
						}

						final WorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllBuckets().stream().filter(bucket -> bucket.getImp().getId().getTable().equals(tableId)))
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.isNotEmpty();
					}
				}

				log.info("Executing query after re-import");

				// Issue a query and assert that it has the same content as the first time around.
				ConceptUpdateAndDeletionTest.assertQueryResult(conquery2, query, 2L, ExecutionState.DONE);
			}
		}
	}
}
