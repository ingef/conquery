package com.bakdata.conquery.integration.tests.deletion;

import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.auth.DevAuthConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveConcept;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test if Imports can be deleted and safely queried.
 *
 */
@Slf4j
public class ConceptUpdateAndDeletionTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		MasterMetaStorage storage = null;

		final DatasetId dataset;
		final Namespace namespace;

		final ConceptId conceptId;

		final QueryTest test, test2;
		final IQuery query;


		try (StandaloneSupport conquery = testConquery.getSupport(name)) {
			storage = conquery.getStandaloneCommand().getMaster().getStorage();

			// Read two JSONs with different Trees
			final String testJson = In.resource("/tests/query/UPDATE_CONCEPT_TESTS/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();
			final String testJson2 = In.resource("/tests/query/UPDATE_CONCEPT_TESTS/SIMPLE_TREECONCEPT_2_Query.json").withUTF8().readAll();

			dataset = conquery.getDataset().getId();
			namespace = storage.getNamespaces().get(dataset);

			conceptId = ConceptId.Parser.INSTANCE.parse(dataset.getName(), "test_tree");

			test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);
			test2 = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson2);

			query = test.parseQuery(conquery);

			// Manually import data, so we can do our own work.
			{
				ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

				test.importTables(conquery);
				conquery.waitUntilWorkDone();

				test.importConcepts(conquery);
				conquery.waitUntilWorkDone();

				test.importTableContents(conquery, Arrays.asList(test.getContent().getTables()));
				conquery.waitUntilWorkDone();
			}

			log.info("Checking state before deletion");

			// State before deletion.
			{
				// Must contain the concept.
				assertThat(namespace.getStorage().getAllConcepts())
						.filteredOn(concept -> concept.getId().equals(conceptId))
						.isNotEmpty();

				assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
						.isNotEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
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

			log.info("Issuing deletion of import {}", conceptId);

			// Delete the Concept.
			namespace.getStorage().removeConcept(conceptId);

			for (WorkerInformation w : namespace.getWorkers()) {
				w.send(new RemoveConcept(conceptId));
			}

			Thread.sleep(100);
			conquery.waitUntilWorkDone();

			log.info("Checking state after deletion");

			{
				// We've deleted the concept so it and it's associated cblock should be gone.
				assertThat(namespace.getStorage().getAllConcepts())
						.filteredOn(concept -> concept.getId().equals(conceptId))
						.isEmpty();

				assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
						.isEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
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
				test2.importConcepts(conquery);
				conquery.waitUntilWorkDone();
			}

			log.info("Checking state after update");

			{
				// Must contain the concept now.
				assertThat(namespace.getStorage().getAllConcepts())
						.filteredOn(concept -> concept.getId().equals(conceptId))
						.isNotEmpty();

				assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
						.isNotEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
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
		}


		// Finally, restart conquery and assert again, that the data is correct.

		//stop dropwizard directly so ConquerySupport does not delete the tmp directory
		testConquery.getDropwizard().after();
		//restart
		testConquery.beforeAll(testConquery.getBeforeAllContext());

		try (StandaloneSupport conquery = testConquery.openDataset(dataset)) {
			log.info("Checking state after re-start");

			{
				// Must contain the concept.
				assertThat(namespace.getStorage().getAllConcepts())
						.filteredOn(concept -> concept.getId().equals(conceptId))
						.isNotEmpty();

				assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
						.isNotEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
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
				assertQueryResult(conquery, query, 2L, ExecutionState.DONE);
			}
		}
	}

	/**
	 * Send a query onto the conquery instance and assert the result's size.
	 */
	private void assertQueryResult(StandaloneSupport conquery, IQuery query, long size, ExecutionState state) throws JSONException {
		final ManagedQuery managedQuery = conquery.getNamespace().getQueryManager().runQuery(query, DevAuthConfig.USER);

		managedQuery.awaitDone(2, TimeUnit.MINUTES);
		assertThat(managedQuery.getState()).isEqualTo(state);

		if(state == ExecutionState.FAILED){
			assertThat(managedQuery.getLastResultCount())
					.describedAs(managedQuery.getResults().toString())
					.isEqualTo(null);
		}else {
			assertThat(managedQuery.getLastResultCount())
					.describedAs(managedQuery.getResults().toString())
					.isEqualTo(size);
		}


	}
}
