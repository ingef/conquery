package com.bakdata.conquery.integration.json;

import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.UriBuilder;

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

	@SneakyThrows
	private static RequiredTable readRequiredTable(String fileResource) {
		return RequiredTable.fromFile(fileResource);
	}

	@Override
	public void importDataset(Client client, UriBuilder adminUriBuilder, String name) {
		Dataset dataset = new Dataset(name);
		dataset.setDataSource("test");
		LoadingUtil.importDataset(client, adminUriBuilder, dataset);
	}
}
