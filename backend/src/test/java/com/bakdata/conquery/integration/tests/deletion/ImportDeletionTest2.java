package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.ConqueryConstants.ALL_IDS_TABLE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
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
public class ImportDeletionTest2 implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {


		final StandaloneSupport conquery = testConquery.getSupport(name);
		MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();

		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		final DatasetId dataset = conquery.getDataset().getId();
		final Namespace namespace = storage.getNamespaces().get(dataset);

		final ImportId importId = ImportId.Parser.INSTANCE.parse(dataset.getName(), "test_table2", "test_table2_import");

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


		// Delete the import.
		{
			log.info("Issuing deletion of import {}", importId);

			conquery.getDatasetsProcessor().deleteImport(importId);

			Thread.sleep(100);
			conquery.waitUntilWorkDone();

		}

		// Manually re-import data, so we can do our own work.
		{


			IntegrationUtils.importTableContents(conquery, Arrays.asList(test.getContent().getTables()), conquery.getDataset());
			conquery.waitUntilWorkDone();
		}

		// State after reimport.
		{
			log.info("Checking state after re-import");

			// TableId.Parser.INSTANCE.parse(namespace.getDataset().getName() + "." + ALL_IDS_TABLE)

			final long size = namespace.getStorage().getAllImports()
														.stream()
														.filter(imp -> imp.getTable().getTable().equalsIgnoreCase(ALL_IDS_TABLE))
														.mapToLong(Import::getNumberOfEntries)
							  .sum();

			assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(nImports);

			for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
				for (Worker worker : slave.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().getDataset().equals(dataset)) {
						continue;
					}

					final WorkerStorage workerStorage = worker.getStorage();

					assertThat(workerStorage.getAllBuckets())
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.filteredOn(bucket -> bucket.getImp().getId().equals(importId))
							.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset))
							.isNotEmpty();
				}
			}

			log.info("Executing query after re-import");

			// Issue a query and assert that it has the same content as the first time around.
			ConceptUpdateAndDeletionTest.assertQueryResult(conquery, query, 2L, ExecutionState.DONE);
		}


	}

}
