package com.bakdata.conquery.integration.json;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SqlTestDataImporter implements TestDataImporter {

	private static final RequiredTable ALL_IDS_TABLE = importRequiredTable("/shared/entities.table.json");

	private final CsvTableImporter csvTableImporter;

	@Override
	public void importQueryTestData(StandaloneSupport support, QueryTest test) throws Exception {
		RequiredData content = test.getContent();
		content.getTables().add(ALL_IDS_TABLE);
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
		content.getTables().add(ALL_IDS_TABLE);
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
		TestDataImporter.super.importTables(support, tables, autoConcept);
	}

	@Override
	public void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		for (RequiredTable table : tables) {
			csvTableImporter.importTableIntoDatabase(table);
		}
	}

	@SneakyThrows
	private static RequiredTable importRequiredTable(String fileResource) {
		return RequiredTable.fromFile(fileResource);
	}

}
