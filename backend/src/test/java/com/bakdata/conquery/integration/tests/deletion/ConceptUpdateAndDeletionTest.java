package com.bakdata.conquery.integration.tests.deletion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveConcept;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

/**
 * Test if Imports can be deleted and safely queried.
 */
@Slf4j
public class ConceptUpdateAndDeletionTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {


		final StandaloneSupport conquery = testConquery.getSupport(name);

		final MetaStorage storage = conquery.getMetaStorage();

		// Read two JSONs with different Trees
		final String testJson = In.resource("/tests/query/UPDATE_CONCEPT_TESTS/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();
		final String testJson2 = In.resource("/tests/query/UPDATE_CONCEPT_TESTS/SIMPLE_TREECONCEPT_2_Query.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final Namespace namespace = storage.getNamespaces().get(dataset.getId());

		final ConceptId conceptId = ConceptId.Parser.INSTANCE.parse(dataset.getName(), "test_tree");

		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);
		final QueryTest test2 = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson2);

		final IQuery query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			LoadingUtil.importTables(conquery, test.getContent());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, test.getContent().getTables(), conquery.getDataset());
			conquery.waitUntilWorkDone();
		}


		// State before deletion.
		{
			log.info("Checking state before deletion");

			// Must contain the concept.
			assertThat(namespace.getStorage().getAllConcepts())
					.filteredOn(concept -> concept.getId().equals(conceptId))
					.isNotEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
					.isNotEmpty();

			for (ShardNode slave : conquery.getShardNodes()) {
				for (Worker value : slave.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset)) {
						continue;
					}

					final WorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getCentralRegistry().getOptional(conceptId))
							.isNotEmpty();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getConnector().getConcept().equals(conceptId))
							.isNotEmpty();
				}
			}

			log.info("Executing query before deletion");

			assertQueryResult(conquery, query, 1L, ExecutionState.DONE);
		}

		// Delete the Concept.
		{
			log.info("Issuing deletion of import {}", conceptId);

			namespace.getStorage().removeConcept(conceptId);

			for (WorkerInformation w : namespace.getWorkers()) {
				w.send(new RemoveConcept(conceptId));
			}

			Thread.sleep(100);
			conquery.waitUntilWorkDone();
		}

		// Check state after deletion.
		{
			log.info("Checking state after deletion");

			// We've deleted the concept so it and it's associated cblock should be gone.
			assertThat(namespace.getStorage().getAllConcepts())
					.filteredOn(concept -> concept.getId().equals(conceptId))
					.isEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
					.isEmpty();

			for (ShardNode slave : conquery.getShardNodes()) {
				for (Worker value : slave.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset)) {
						continue;
					}


					final WorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getCentralRegistry().getOptional(conceptId))
							.isEmpty();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getConnector().getConcept().equals(conceptId))
							.isEmpty();
				}
			}

			log.info("Executing query after deletion (EXPECTING AN EXCEPTION IN THE LOGS!)");

			// Issue a query and assert that it is failing.
			assertQueryResult(conquery, query, 0L, ExecutionState.FAILED);
		}

		conquery.waitUntilWorkDone();

		// Load a different concept with the same id (it has different children "C1" that are more than "A1")
		{
			// only import the deleted concept
			LoadingUtil.importConcepts(conquery, test2.getRawConcepts());
			conquery.waitUntilWorkDone();
		}

		// Check state after update.
		{
			log.info("Checking state after update");

			// Must contain the concept now.
			assertThat(namespace.getStorage().getAllConcepts())
					.filteredOn(concept -> concept.getId().equals(conceptId))
					.isNotEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
					.isNotEmpty();

			for (ShardNode slave : conquery.getShardNodes()) {
				for (Worker value : slave.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset)) {
						continue;
					}

					final WorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getCentralRegistry().getOptional(conceptId))
							.isNotEmpty();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getConnector().getConcept().equals(conceptId))
							.isNotEmpty();
				}
			}

			log.info("Executing query after update");

			// Assert that it now contains 2 instead of 1.
			assertQueryResult(conquery, query, 2L, ExecutionState.DONE);
		}

		// Finally, restart conquery and assert again, that the data is correct.
		{
			//stop dropwizard directly so ConquerySupport does not delete the tmp directory
			testConquery.getDropwizard().after();
			//restart
			testConquery.beforeAll(testConquery.getBeforeAllContext());

			StandaloneSupport conquery2 = testConquery.openDataset(dataset.getId());

			log.info("Checking state after re-start");

			{
				// Must contain the concept.
				assertThat(namespace.getStorage().getAllConcepts())
						.filteredOn(concept -> concept.getId().equals(conceptId))
						.isNotEmpty();

				assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
						.isNotEmpty();

				for (ShardNode slave : conquery2.getShardNodes()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						if (!value.getInfo().getDataset().equals(dataset)) {
							continue;
						}

						final WorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getCentralRegistry().getOptional(conceptId))
								.isNotEmpty();

						assertThat(workerStorage.getAllCBlocks())
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.filteredOn(cBlock -> cBlock.getConnector().getConcept().equals(conceptId))
								.isNotEmpty();
					}
				}

				log.info("Executing query after restart.");
				// Re-assert state.
				assertQueryResult(conquery2, query, 2L, ExecutionState.DONE);
			}
		}
	}

	/**
	 * Send a query onto the conquery instance and assert the result's size.
	 */
	public static void assertQueryResult(StandaloneSupport conquery, IQuery query, long size, ExecutionState state) {
		Namespaces namespaces = conquery.getNamespace().getNamespaces();
		MetaStorage storage = conquery.getNamespace().getStorage().getMetaStorage();
		UserId userId = conquery.getTestUser().getId();
		DatasetId dataset = conquery.getNamespace().getDataset().getId();
		
		final ManagedQuery managedQuery = (ManagedQuery) ExecutionManager.runQuery(namespaces, query, userId, dataset);

		managedQuery.awaitDone(2, TimeUnit.MINUTES);
		assertThat(managedQuery.getState()).isEqualTo(state);

		if (state == ExecutionState.DONE) {
			assertThat(managedQuery.getLastResultCount())
					.describedAs(managedQuery.getResults().toString())
					.isEqualTo(size);
		}
	}
}
