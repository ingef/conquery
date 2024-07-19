package com.bakdata.conquery.integration.json;

import java.util.Collection;

import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SqlTestDataImporter implements TestDataImporter {

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
	}

	@Override
	public void importFilterTestData(StandaloneSupport support, FilterTest filterTest) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		for (RequiredTable table : tables) {
			csvTableImporter.importTableIntoDatabase(table);
		}
	}

}
