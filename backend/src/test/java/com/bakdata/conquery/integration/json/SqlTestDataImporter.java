package com.bakdata.conquery.integration.json;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SqlTestDataImporter extends WorkerTestDataImporter {

	private final CsvTableImporter csvTableImporter;


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
}
