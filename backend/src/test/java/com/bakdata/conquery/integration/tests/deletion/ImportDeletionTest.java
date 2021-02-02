package com.bakdata.conquery.integration.tests.deletion;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
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
		MetaStorage storage = conquery.getMetaStorage();

		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final Namespace namespace = storage.getDatasetRegistry().get(dataset.getId());

		final ImportId importId = ImportId.Parser.INSTANCE.parse(dataset.getName(), "test_table2", "test_table2_import");

		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);
		final IQuery query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());

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

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker worker : node.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", worker.getInfo().getId())
							.filteredOn(block -> block.getBucket().getDataset().equals(dataset.getId()))
							.isNotEmpty();
					assertThat(workerStorage.getAllBuckets())
							.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset.getId()))
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.isNotEmpty();

					// Must contain the import.
					assertThat(workerStorage.getImport(importId))
							.isNotNull();
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
			assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(nImports - 1);

			// The deleted import should not be found.
			assertThat(namespace.getStorage().getAllImports())
					.filteredOn(imp -> imp.getId().equals(importId))
					.isEmpty();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker worker : node.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

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
					
					// Import should not exists anymore
					assertThat(workerStorage.getImport(importId))
					.describedAs("Import for Worker %s", worker.getInfo().getId())
					.isNull();
				}
			}

			log.info("Executing query after deletion");

			// Issue a query and assert that it has less content.
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

			//create import descriptor
			InputFile inputFile = InputFile.fromName(conquery.getConfig().getPreprocessor().getDirectories()[0], importId.getTag(), null);
			TableImportDescriptor desc = new TableImportDescriptor();
			desc.setInputFile(inputFile);
			desc.setName(import2Table.getName() + "_import");
			desc.setTable(import2Table.getName());
			TableInputDescriptor input = new TableInputDescriptor();
			{
				input.setPrimary(IntegrationUtils.copyOutput(import2Table.getPrimaryColumn()));
				input.setSourceFile(new File(inputFile.getCsvDirectory(), import2Table.getCsv().getName()));
				input.setOutput(new OutputDescription[import2Table.getColumns().length]);
				for (int i = 0; i < import2Table.getColumns().length; i++) {
					input.getOutput()[i] = IntegrationUtils.copyOutput(import2Table.getColumns()[i]);
				}
			}
			desc.setInputs(new TableInputDescriptor[]{input});
			Jackson.MAPPER.writeValue(inputFile.getDescriptionFile(), desc);

			//preprocess
			conquery.preprocessTmp();

			//import preprocessedFiles
			conquery.getDatasetsProcessor().addImport(conquery.getNamespace(), inputFile.getPreprocessedFile());
			conquery.waitUntilWorkDone();
		}

		// State after reimport.
		{
			log.info("Checking state after re-import");

			assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(nImports);

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker worker : node.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

					assertThat(workerStorage.getAllBuckets())
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.filteredOn(bucket -> bucket.getImp().getId().equals(importId))
							.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset.getId()))
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

			StandaloneSupport conquery2 = testConquery.openDataset(dataset.getId());
			log.info("Checking state after re-start");

			{
				assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(2);

				for (ShardNode node : conquery2.getShardNodes()) {
					for (Worker worker : node.getWorkers().getWorkers().values()) {

						if (!worker.getInfo().getDataset().equals(dataset.getId()))
							continue;

						final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

						assertThat(workerStorage.getAllBuckets())
								.describedAs("Buckets for Worker %s", worker.getInfo().getId())
								.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset.getId()))
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
