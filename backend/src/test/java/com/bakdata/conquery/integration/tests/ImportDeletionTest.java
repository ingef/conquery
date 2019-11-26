package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.auth.DevAuthConfig;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveImportJob;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.bakdata.conquery.models.preproc.Input;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test if Imports can be deleted and safely queried.
 *
 */
@Slf4j
public class ImportDeletionTest implements ProgrammaticIntegrationTest {

	private Mandator mandator = new Mandator("testMandatorName", "testMandatorLabel");
	private MandatorId mandatorId = mandator.getId();
	private User user = new User("testUser@test.de", "testUserName");
	private UserId userId = user.getId();
	private ConqueryPermission permission = new DatasetPermission(Ability.READ.asSet(), new DatasetId("testDatasetId"));

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		MasterMetaStorage storage = null;

		try (StandaloneSupport conquery = testConquery.getSupport(name)){
			storage = conquery.getStandaloneCommand().getMaster().getStorage();

			final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

			final DatasetId dataset = conquery.getDataset().getId();
			final Namespace namespace = storage.getNamespaces().get(dataset);

			final ImportId importId = ImportId.Parser.INSTANCE.parse(dataset.getName(), "test_table2", "test_table2_import");

			final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);
			final IQuery query = test.parseQuery(conquery);

			// Manually import data, so we can do our own work.
			{
				ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

				test.importTables(conquery);
				conquery.waitUntilWorkDone();

				test.importConcepts(conquery);
				conquery.waitUntilWorkDone();

				importTableContents(conquery, Arrays.asList(test.getContent().getTables()));
				conquery.waitUntilWorkDone();
			}




			final int nImports = namespace.getStorage().getAllImports().size();

			log.info("Checking state before deletion");

			// State before deletion.
			{
				// Must contain the import.
				assertThat(namespace.getStorage().getAllImports())
						.filteredOn(imp -> imp.getId().equals(importId))
						.isNotEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
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

				assertQueryResult(conquery, query, 2L);
			}

			log.info("Issuing deletion of import {}", importId);

			// Delete the import.
			namespace.getStorage().removeImport(importId);

			for (WorkerInformation w : namespace.getWorkers()) {
				w.send(new RemoveImportJob(importId));
			}
			Thread.sleep(100);
			conquery.waitUntilWorkDone();

			log.info("Checking state after deletion");

			{
				// We have deleted an import now there should be two less!
				assertThat(namespace.getStorage().getAllImports().size()).isLessThan(nImports);

				// The deleted import should not be found.
				assertThat(namespace.getStorage().getAllImports())
						.filteredOn(imp -> imp.getId().equals(importId))
						.isEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						final WorkerStorage workerStorage = value.getStorage();

						// No bucket should be found referencing the import.
						assertThat(workerStorage.getAllBuckets())
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.filteredOn(bucket -> bucket.getImp().getId().equals(importId))
								.isEmpty();

						// No CBlock associated with import may exist
						assertThat(workerStorage.getAllCBlocks())
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.filteredOn(cBlock -> cBlock.getBucket().getImp().equals(importId))
								.isEmpty();
					}
				}

				log.info("Executing query after deletion");

				// Issue a query and asseert that it has less content.
				assertQueryResult(conquery, query, 1L);
			}

			conquery.waitUntilWorkDone();

			// Load another import into the same table, with only the deleted import/table
			{
				// only import the deleted import/table
				importTableContents(conquery, Arrays.stream(test.getContent().getTables()).filter(table -> table.getName().equalsIgnoreCase(importId.getTable().getTable())).collect(Collectors.toList()));
				conquery.waitUntilWorkDone();
			}

			log.info("Checking state after re-import");

			{
				assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(nImports);

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						final WorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllBuckets().stream().filter(bucket -> bucket.getImp().getId().equals(importId)))
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.isNotEmpty();
					}
				}

				log.info("Executing query after re-import");

				// Issue a query and assert that it has less content.
				assertQueryResult(conquery, query, 2);
			}

		}
		finally {
			if (storage != null) {
				storage.removeMandator(mandatorId);
				storage.removeUser(userId);
			}
		}
	}

	private void assertQueryResult(StandaloneSupport conquery, IQuery query, long size) throws JSONException {
		final ManagedQuery managedQuery = conquery.getNamespace().getQueryManager().runQuery(query, DevAuthConfig.USER);

		managedQuery.awaitDone(2, TimeUnit.MINUTES);
		assertThat(managedQuery.getState()).isEqualTo(ExecutionState.DONE);

		assertThat(managedQuery.getLastResultCount()).isEqualTo(size);
	}

	public void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws IOException, JSONException {
		CsvParserSettings settings = new CsvParserSettings();
		CsvFormat format = new CsvFormat();
		format.setLineSeparator("\n");
		settings.setFormat(format);
		settings.setHeaderExtractionEnabled(true);
		DateFormats.initialize(ArrayUtils.EMPTY_STRING_ARRAY);
		List<File> preprocessedFiles = new ArrayList<>();

		for (RequiredTable rTable : tables) {
			//copy csv to tmp folder
			String name = rTable.getCsv().getName().substring(0, rTable.getCsv().getName().lastIndexOf('.'));
			FileUtils.copyInputStreamToFile(rTable.getCsv().stream(), new File(support.getTmpDir(), rTable.getCsv().getName()));

			//create import descriptor
			InputFile inputFile = InputFile.fromName(support.getConfig().getPreprocessor().getDirectories()[0], name);
			ImportDescriptor desc = new ImportDescriptor();
			desc.setInputFile(inputFile);
			desc.setName(rTable.getName() + "_import");
			desc.setTable(rTable.getName());
			Input input = new Input();
			{
				input.setPrimary(QueryTest.copyOutput(0, rTable.getPrimaryColumn()));
				input.setSourceFile(new File(inputFile.getCsvDirectory(), rTable.getCsv().getName()));
				input.setOutput(new Output[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = QueryTest.copyOutput(i + 1, rTable.getColumns()[i]);
				}
			}
			desc.setInputs(new Input[]{input});
			Jackson.MAPPER.writeValue(inputFile.getDescriptionFile(), desc);
			preprocessedFiles.add(inputFile.getPreprocessedFile());
		}
		//preprocess
		support.preprocessTmp();

		//import preprocessedFiles
		for (File file : preprocessedFiles) {
			support.getDatasetsProcessor().addImport(support.getDataset(), file);
		}
	}
}
