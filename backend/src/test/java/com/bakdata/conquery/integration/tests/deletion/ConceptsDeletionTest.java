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
public class ConceptsDeletionTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		MasterMetaStorage storage = null;

		final DatasetId dataset;
		final Namespace namespace;

		final ConceptId conceptId;

		final QueryTest test;
		final IQuery query;


		try (StandaloneSupport conquery = testConquery.getSupport(name)) {
			storage = conquery.getStandaloneCommand().getMaster().getStorage();

			final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

			dataset = conquery.getDataset().getId();
			namespace = storage.getNamespaces().get(dataset);

			conceptId = ConceptId.Parser.INSTANCE.parse(dataset.getName(), "test_tree");

			test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);
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

			final int nImports = namespace.getStorage().getAllImports().size();

			log.info("Checking state before deletion");

			// State before deletion.
			{
				// Must contain the concept.
				assertThat(namespace.getStorage().getAllConcepts())
						.filteredOn(concept -> concept.getId().equals(conceptId))
						.isNotEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						final WorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllCBlocks())
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.filteredOn(cBlock -> cBlock.getConnector().getConcept().equals(conceptId))
								.isNotEmpty();
					}
				}

				log.info("Executing query before deletion");

				assertQueryResult(conquery, query, 2L, ExecutionState.DONE);
			}

			log.info("Issuing deletion of import {}", conceptId);

			// Delete the import.
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

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						final WorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllCBlocks())
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.filteredOn(cBlock -> cBlock.getConnector().getConcept().equals(conceptId))
								.isEmpty();
					}
				}

				log.info("Executing query after deletion (EXPECTING AN EXCEPTION IN THE LOGS!)");

				// Issue a query and assert that it is failing..
				assertQueryResult(conquery, query, 0L, ExecutionState.FAILED);
			}

			conquery.waitUntilWorkDone();

			// Load the same import into the same table, with only the deleted import/table
			{
				// only import the deleted concept
				test.importConcepts(conquery);
				conquery.waitUntilWorkDone();
			}

			log.info("Checking state after re-import");

			{
				// Must contain the concept.
				assertThat(namespace.getStorage().getAllConcepts())
						.filteredOn(concept -> concept.getId().equals(conceptId))
						.isNotEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						final WorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllCBlocks())
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.filteredOn(cBlock -> cBlock.getConnector().getConcept().equals(conceptId))
								.isNotEmpty();
					}
				}

				log.info("Executing query before deletion");

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

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						final WorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllCBlocks())
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.filteredOn(cBlock -> cBlock.getConnector().getConcept().equals(conceptId))
								.isNotEmpty();
					}
				}

				log.info("Executing query after restart.");

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
			assertThat(managedQuery.getLastResultCount()).isEqualTo(null);
		}else {
			assertThat(managedQuery.getLastResultCount()).isEqualTo(size);
		}


	}
}
