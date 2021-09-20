package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ImportUpdateTest implements ProgrammaticIntegrationTest {
	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);
		MetaStorage storage = conquery.getMetaStorage();

		String testJson = In.resource("/tests/query/UPDATE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final Namespace namespace = storage.getDatasetRegistry().get(dataset.getId());

		final ImportId importId1 = ImportId.Parser.INSTANCE.parse(dataset.getName(), "test_table", "test_table");
		final ImportId importId2 = ImportId.Parser.INSTANCE.parse(dataset.getName(), "test_table2", "test_table2");
		final ImportId importId3 = ImportId.Parser.INSTANCE.parse(dataset.getName(), "test_table3", "test_table3");

		QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);

		List<File> cqpps;

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, test.getContent().getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, test.getContent().getTables());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			cqpps = LoadingUtil.generateCqpp(conquery, test.getContent().getTables());
			conquery.waitUntilWorkDone();

			LoadingUtil.importCqppFiles(conquery,List.of(cqpps.get(0)));
			conquery.waitUntilWorkDone();
		}


		// State before update.
		{
			log.info("Checking state before update");

			// Must contain the import.
			assertThat(namespace.getStorage().getAllImports())
					.filteredOn(imp -> imp.getId().equals(importId2))
					.isNotEmpty();

			assertThat(namespace.getStorage().getCentralRegistry().getOptional(importId2))
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
					assertThat(workerStorage.getImport(importId2))
							.isNotNull();
				}
			}

		}

		//Update import
		{

			//different ImportIds
			LoadingUtil.updateCqppFile(conquery, cqpps.get(0), importId1, Response.Status.Family.CLIENT_ERROR, "Conflict");
			conquery.waitUntilWorkDone();

			//same ImportIds but Import is not present
			LoadingUtil.updateCqppFile(conquery, cqpps.get(1), importId3, Response.Status.Family.CLIENT_ERROR, "Not Found");
			conquery.waitUntilWorkDone();

			//correct update of import
			LoadingUtil.updateCqppFile(conquery, cqpps.get(0), importId2, Response.Status.Family.SUCCESSFUL, "No Content");
			conquery.waitUntilWorkDone();
		}
	}
}
