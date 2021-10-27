package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

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
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
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
public class ConceptUpdateAndDeletionTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {


		final StandaloneSupport conquery = testConquery.getSupport(name);

		final MetaStorage storage = conquery.getMetaStorage();

		// Read two JSONs with different Trees
		final String testJson = In.resource("/tests/query/UPDATE_CONCEPT_TESTS/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();
		final String testJson2 = In.resource("/tests/query/UPDATE_CONCEPT_TESTS/SIMPLE_TREECONCEPT_2_Query.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final Namespace namespace = conquery.getNamespace();

		final ConceptId conceptId = ConceptId.Parser.INSTANCE.parse(dataset.getName(), "test_tree");
		final Concept<?> concept;

		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);
		final QueryTest test2 = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson2);

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, test.getContent().getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, test.getContent().getTables());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			assertThat(namespace.getStorage().getConcept(conceptId)).isNotNull();

			LoadingUtil.importTableContents(conquery, test.getContent().getTables());
			conquery.waitUntilWorkDone();
		}

		final Query query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());


		// State before update.
		{
			log.info("Checking state before deletion");

			// Must contain the concept.
			assertThat(namespace.getStorage().getAllConcepts())
					.filteredOn(con -> con.getId().equals(conceptId))
					.isNotEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
					.isNotEmpty();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getCentralRegistry().getOptional(conceptId))
							.isNotEmpty();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getConnector().getConcept().getId().equals(conceptId))
							.isNotEmpty();
				}
			}

			log.info("Executing query before deletion");

			IntegrationUtils.assertQueryResult(conquery, query, 1L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}
		conquery.waitUntilWorkDone();

		// Load a different concept with the same id (it has different children "C1" that are more than "A1")
		// To perform the update, the old concept will be deleted first and the new concept will be added. That means the deletion of concept is also covered here
		{
			LoadingUtil.updateConcepts(conquery, test2.getRawConcepts(), Response.Status.Family.SUCCESSFUL, "No Content");
		}
		conquery.waitUntilWorkDone();

		// Check state after update.
		{
			log.info("Checking state after update");

			// Must contain the concept now.
			assertThat(namespace.getStorage().getAllConcepts())
					.filteredOn(con -> con.getId().equals(conceptId))
					.isNotEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
					.isNotEmpty();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker value : node.getWorkers().getWorkers().values()) {
					if (!value.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

					assertThat(workerStorage.getCentralRegistry().getOptional(conceptId))
							.isNotEmpty();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", value.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getConnector().getConcept().getId().equals(conceptId))
							.isNotEmpty();
				}
			}

			log.info("Executing query after update");

			// Assert that it now contains 2 instead of 1.
			IntegrationUtils.assertQueryResult(conquery, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
			conquery.waitUntilWorkDone();
		}



		// Restart conquery and assert again, that the data is correct.
		{
			testConquery.shutdown();
			//restart
			testConquery.beforeAll();

			StandaloneSupport conquery2 = testConquery.openDataset(dataset.getId());

			log.info("Checking state after re-start");

			{
				// Must contain the concept.
				assertThat(namespace.getStorage().getAllConcepts())
						.filteredOn(con -> con.getId().equals(conceptId))
						.isNotEmpty();

				assertThat(conquery2.getNamespace().getStorage().getCentralRegistry().getOptional(conceptId))
						.isNotEmpty();

				for (ShardNode node : conquery2.getShardNodes()) {
					for (Worker value : node.getWorkers().getWorkers().values()) {
						if (!value.getInfo().getDataset().equals(dataset.getId())) {
							continue;
						}

						final ModificationShieldedWorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getCentralRegistry().getOptional(conceptId))
								.isNotEmpty();

						assertThat(workerStorage.getAllCBlocks())
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.filteredOn(cBlock -> cBlock.getConnector().getConcept().getId().equals(conceptId))
								.isNotEmpty();
					}
				}

				log.info("Executing query after restart.");
				// Re-assert state.
				IntegrationUtils.assertQueryResult(conquery2, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
			}
		}


		// Delete the Concept.
		{
			log.info("Issuing deletion of import {}", conceptId);
			concept = Objects.requireNonNull(namespace.getStorage().getConcept(conceptId));
			conquery.getDatasetsProcessor().deleteConcept(concept);

			conquery.waitUntilWorkDone();
		}

		// Check state after deletion.
		{
			log.info("Checking state after deletion");

			// We've deleted the concept so it and it's associated cblock should be gone.
			assertThat(namespace.getStorage().getAllConcepts())
					.filteredOn(con -> con.getId().equals(conceptId))
					.isEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(conceptId))
					.isEmpty();

			assertThat(
					conquery.getShardNodes().stream()
							.flatMap(node -> node.getWorkers().getWorkers().values().stream())
							.filter(worker -> worker.getInfo().getDataset().equals(dataset.getId()))
							.map(Worker::getStorage)
			)
					// Concept is deleted on Workers
					.noneMatch(workerStorage -> workerStorage.getConcept(conceptId) != null)
					// CBlocks of Concept are deleted on Workers
					.noneMatch(workerStorage -> workerStorage.getAllCBlocks()
															 .stream()
															 .anyMatch(cBlock -> cBlock.getConnector().getConcept().getId().equals(conceptId)));


			log.info("Executing query after deletion (EXPECTING AN EXCEPTION IN THE LOGS!)");

			// Issue a query and assert that it is failing.
			IntegrationUtils.assertQueryResult(conquery, query, 0L, ExecutionState.FAILED, conquery.getTestUser(), 400);
		}
	}

}
