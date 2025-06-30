package com.bakdata.conquery.integration.json;

import java.util.Collection;

import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SqlTestDataImporter extends WorkerTestDataImporter {

	private final CsvTableImporter csvTableImporter;


	@Override
	public void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		csvTableImporter.importAllIds(tables);

		for (RequiredTable table : tables) {
			csvTableImporter.importTableIntoDatabase(table);
		}
	}
}
