package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import javax.ws.rs.core.Response;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public class ImportUpdateTest implements ProgrammaticIntegrationTest {
	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);
		MetaStorage storage = conquery.getMetaStorage();

		String testJson = In.resource("/tests/query/UPDATE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final Namespace namespace = conquery.getNamespace();

		final ImportId importId1 = ImportId.Parser.INSTANCE.parse(dataset.getName(), "table1", "table1");
		final ImportId importId2 = ImportId.Parser.INSTANCE.parse(dataset.getName(), "table2", "table2");

		QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);

		final List<RequiredTable> tables = test.getContent().getTables();
		assertThat(tables.size()).isEqualTo(2);

		List<File> cqpps;

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, test.getContent().getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, tables);
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			cqpps = LoadingUtil.generateCqpp(conquery, tables);
			conquery.waitUntilWorkDone();

			assertThat(cqpps.size()).isEqualTo(tables.size());

			LoadingUtil.importCqppFiles(conquery, List.of(cqpps.get(0)));
			conquery.waitUntilWorkDone();
		}
		final Query query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());

		// State before update.
		{
			log.info("Checking state before update");
			assertThat(namespace.getStorage().getAllImports()).hasSize(1);
			// Must contain the import.
			assertThat(namespace.getStorage().getAllImports())
					.filteredOn(imp -> imp.getId().equals(importId1))
					.isNotEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(importId1))
					.isNotEmpty();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker worker : node.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", worker.getInfo().getId())
							.filteredOn(block -> block.getBucket().getId().getDataset().equals(dataset.getId()))
							.isNotEmpty();

					assertThat(workerStorage.getAllBuckets())
							.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset.getId()))
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.isNotEmpty();

					// Must contain the import.
					assertThat(workerStorage.getImport(importId1))
							.isNotNull();
				}
			}
			assertThat(namespace.getNumberOfEntities()).isEqualTo(4);

			//assert that the query can be executed after the import
			IntegrationUtils.assertQueryResult(conquery, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);

		}

		//Try to update an import that does not exist should throw a Not-Found Webapplication Exception
		LoadingUtil.updateCqppFile(conquery, cqpps.get(1), Response.Status.Family.CLIENT_ERROR, "Not Found");
		conquery.waitUntilWorkDone();

		//Load manually new data for import and update the concerned import
		{
			log.info("Manually loading new data for import");

			final RequiredTable importTable = test.getContent().getTables().stream()
												  .filter(table -> table.getName().equalsIgnoreCase(importId1.getTable().getTable()))
												  .findFirst()
												  .orElseThrow();

			final String csvName = importTable.getCsv().getName();
			final String path = importTable.getCsv().getPath();

			//copy new content of the importTable into the csv-File used by the preprocessor to avoid creating multiple files withe same names
			FileUtils.copyInputStreamToFile(In.resource(path.substring(0, path.lastIndexOf('/')) + "/" + csvName.replace(".csv", ".update.csv"))
											  .asStream(), new File(conquery.getTmpDir(), csvName));

			File descriptionFile = new File(conquery.getTmpDir(), importTable.getName() + ConqueryConstants.EXTENSION_DESCRIPTION);
			File newPreprocessedFile = new File(conquery.getTmpDir(), importTable.getName() + ConqueryConstants.EXTENSION_PREPROCESSED);

			//create import descriptor
			{
				TableImportDescriptor desc = new TableImportDescriptor();
				desc.setName(importTable.getName());
				desc.setTable(importTable.getName());
				TableInputDescriptor input = new TableInputDescriptor();
				{
					input.setPrimary(importTable.getPrimaryColumn().createOutput());
					input.setSourceFile(csvName);
					input.setOutput(new OutputDescription[importTable.getColumns().length]);
					for (int i = 0; i < importTable.getColumns().length; i++) {
						input.getOutput()[i] = importTable.getColumns()[i].createOutput();
					}
				}
				desc.setInputs(new TableInputDescriptor[]{input});
				Jackson.getMapper().writeValue(descriptionFile, desc);
			}

			//preprocess
			conquery.preprocessTmp(conquery.getTmpDir(), List.of(descriptionFile));

			log.info("updating import");
			//correct update of the import
			LoadingUtil.updateCqppFile(conquery, newPreprocessedFile, Response.Status.Family.SUCCESSFUL, "No Content");
			conquery.waitUntilWorkDone();
		}

		// State after update.
		{
			log.info("Checking state after update");
			assertThat(namespace.getStorage().getAllImports()).hasSize(1);
			// Must contain the import.
			assertThat(namespace.getStorage().getAllImports())
					.filteredOn(imp -> imp.getId().equals(importId1))
					.isNotEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(importId1))
					.isNotEmpty();

			for (ShardNode node : conquery.getShardNodes()) {
				for (Worker worker : node.getWorkers().getWorkers().values()) {
					if (!worker.getInfo().getDataset().equals(dataset.getId())) {
						continue;
					}

					final ModificationShieldedWorkerStorage workerStorage = worker.getStorage();

					assertThat(workerStorage.getAllCBlocks())
							.describedAs("CBlocks for Worker %s", worker.getInfo().getId())
							.filteredOn(block -> block.getBucket().getId().getDataset().equals(dataset.getId()))
							.isNotEmpty();

					assertThat(workerStorage.getAllBuckets())
							.filteredOn(bucket -> bucket.getId().getDataset().equals(dataset.getId()))
							.describedAs("Buckets for Worker %s", worker.getInfo().getId())
							.isNotEmpty();

					// Must contain the import.
					assertThat(workerStorage.getImport(importId1))
							.isNotNull();
				}
			}
			assertThat(namespace.getNumberOfEntities()).isEqualTo(9);
			// Issue a query and assert that it has more content.
			IntegrationUtils.assertQueryResult(conquery, query, 4L, ExecutionState.DONE, conquery.getTestUser(), 201);


		}
	}
}
