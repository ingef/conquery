package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.ConqueryConstants.EXTENSION_PREPROCESSED;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

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
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.preproc.Preprocessor;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;



/**
 * Test if Imports can be deleted and safely queried.
 */
@Slf4j
public class ImportDeletionTest implements ProgrammaticIntegrationTest {


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

			IntegrationUtils.importTableContents(conquery, test.getContent().getTables(), conquery.getDataset());
			conquery.waitUntilWorkDone();
		}

		final int nImports = namespace.getStorage().getAllImports().size();


		// State before deletion.
		{
			log.info("Checking state before deletion");

			// Must contain the import.
			assertThat(namespace.getStorage().getAllImports())
					.filteredOn(imp -> imp.getId().equals(importId))
					.isNotEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(importId))
					.isNotEmpty();

			for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
				for (Worker worker : slave.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().getDataset().equals(dataset)) {
						continue;
					}

					final WorkerStorage workerStorage = worker.getStorage();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", worker.getInfo().getId())
							.filteredOn(block -> block.getBucket().getDataset().equals(dataset))
							.isNotEmpty();
					assertThat(workerStorage.getAllBuckets())
							.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset))
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.isNotEmpty();
				}
			}

			log.info("Executing query before deletion");

			ConceptUpdateAndDeletionTest.assertQueryResult(conquery, query, 2L, ExecutionState.DONE);
		}

		// Delete the import.
		{
			log.info("Issuing deletion of import {}", importId);

			conquery.getDatasetsProcessor().deleteImport(importId);

			Thread.sleep(100);
			conquery.waitUntilWorkDone();

		}

		// State after deletion.
		{
			log.info("Checking state after deletion");
			// We have deleted an import now there should be two less!
			assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(nImports - 2);

			// The deleted import should not be found.
			assertThat(namespace.getStorage().getAllImports())
					.filteredOn(imp -> imp.getId().equals(importId))
					.isEmpty();

			for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
				for (Worker worker : slave.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().getDataset().equals(dataset)) {
						continue;
					}

					final WorkerStorage workerStorage = worker.getStorage();

					// No bucket should be found referencing the import.
					assertThat(workerStorage.getAllBuckets())
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.filteredOn(bucket -> bucket.getImp().getId().equals(importId))
							.isEmpty();

					// No CBlock associated with import may exist
					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", worker.getInfo().getId())
							.filteredOn(cBlock -> cBlock.getBucket().getImp().equals(importId))
							.isEmpty();
				}
			}

			log.info("Executing query after deletion");

			// Issue a query and asseert that it has less content.
			ConceptUpdateAndDeletionTest.assertQueryResult(conquery, query, 1L, ExecutionState.DONE);
		}

		conquery.waitUntilWorkDone();


		// Load more data under the same name into the same table, with only the deleted import/table
		{
			// only import the deleted import/table
			final RequiredTable import2Table = test.getContent().getTables().stream()
													 .filter(table -> table.getName().equalsIgnoreCase(importId.getTable().getTable()))
													 .findFirst()
													 .orElseThrow();


			final String path = import2Table.getCsv().getPath();

			//copy csv to tmp folder
			// Content 2.2 contains an extra entry of a value that hasn't been seen before.
			FileUtils.copyInputStreamToFile(In.resource(path.substring(0, path.lastIndexOf("/")) + "/" + "content2.2.csv")
											  .asStream(), new File(conquery.getTmpDir(), import2Table.getCsv().getName()));


			//preprocess
			conquery.preprocessTmp();

			//import preprocessedFiles

			conquery.getDatasetsProcessor().addImport(conquery.getDataset(), Preprocessor.getTaggedVersion(new File(conquery.getTmpDir(), import2Table.getCsv().getName().substring(0, import2Table.getCsv().getName().lastIndexOf('.')) + EXTENSION_PREPROCESSED), null, "csv\\.gz"));
			conquery.waitUntilWorkDone();
		}

		// State after reimport.
		{
			log.info("Checking state after re-import");

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

		// Finally, restart conquery and assert again, that the data is correct.
		{

			//stop dropwizard directly so ConquerySupport does not delete the tmp directory
			testConquery.getDropwizard().after();
			//restart
			testConquery.beforeAll(testConquery.getBeforeAllContext());

			StandaloneSupport conquery2 = testConquery.openDataset(dataset);
			log.info("Checking state after re-start");

			{
				assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(4);

				for (SlaveCommand slave : conquery2.getStandaloneCommand().getSlaves()) {
					for (Worker worker : slave.getWorkers().getWorkers().values()) {

						if (!worker.getInfo().getDataset().getDataset().equals(dataset))
							continue;

						final WorkerStorage workerStorage = worker.getStorage();

						assertThat(workerStorage.getAllBuckets())
								.describedAs("Buckets for Worker %s", worker.getInfo().getId())
								.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset))
								.filteredOn(bucket -> bucket.getImp().getId().equals(importId))
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
