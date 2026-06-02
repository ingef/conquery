package com.bakdata.conquery.integration.json;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.integration.json.filter.FilterTest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static com.bakdata.conquery.ConqueryConstants.ALL_IDS_TABLE;

@Slf4j
@Data
public class SqlTestDataImporter extends WorkerTestDataImporter {

	private final CsvTableImporter csvTableImporter;

	@Override
	public void importQueryTestData(StandaloneSupport support, QueryTest test) throws Exception {
		RequiredData content = test.getContent();

		importSecondaryIds(support, content.getSecondaryIds());
		importTables(support, content.getTables(), true);
		importConcepts(support, test.getRawConcepts());
		importTableContents(support, content.getTables());

		importSearchIndexes(support, test.getSearchIndexes());
		importIdMapping(support, content);
		waitUntilDone(support, () -> LoadingUtil.updateMatchingStats(support));

	}

	@Override
	public void importFormTestData(StandaloneSupport support, FormTest test) throws Exception {
		RequiredData content = test.getContent();

		importSecondaryIds(support, content.getSecondaryIds());
		importTables(support, content.getTables(), true);
		importConcepts(support, test.getRawConcepts());
		importTableContents(support, content.getTables());
		importIdMapping(support, content);
		importPreviousQueries(support, content);
		waitUntilDone(support, () -> LoadingUtil.updateMatchingStats(support));

	}

	@Override
	public void importFilterTestData(StandaloneSupport support, FilterTest filterTest) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public void importTables(StandaloneSupport support, List<RequiredTable> tables, boolean autoConcept) throws JSONException {
		for (RequiredTable table : tables) {
			csvTableImporter.createTable(table);
		}
		super.importTables(support, tables, autoConcept);
	}

	@Override
	public void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		csvTableImporter.importAllIds(tables);

		for (RequiredTable table : tables) {
			csvTableImporter.importTableIntoDatabase(table);
		}
	}

	@Override
	public void importDataset(Client client, UriBuilder adminUriBuilder, String name) {
		Dataset dataset = new Dataset(name);
		dataset.setDataSource("test");
		LoadingUtil.importDataset(client, adminUriBuilder, dataset);
	}
}
