package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.util.List;

import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SqlTestDataImporter implements TestDataImporter {

	private final CsvTableImporter csvTableImporter;

	@Override
	public void importQueryTestData(StandaloneSupport support, QueryTest test) throws Exception {
		RequiredData content = test.getContent();
		importTables(support, content);
		importConcepts(support, test.getRawConcepts());
		for (RequiredTable table : content.getTables()) {
			csvTableImporter.importTableIntoDatabase(table);
		}
	}

	@Override
	public void importFormTestData(StandaloneSupport support, FormTest formTest) throws Exception {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public void importFilterTestData(StandaloneSupport support, FilterTest filterTest) throws Exception {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	private void importTables(StandaloneSupport support, RequiredData content) {
		for (RequiredTable rTable : content.getTables()) {
			final Table table = rTable.toTable(support.getDataset(), support.getNamespaceStorage().getCentralRegistry());
			support.getNamespaceStorage().addTable(table);
		}
	}

	private void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws IOException, JSONException {
		List<Concept<?>> concepts =
				ConqueryTestSpec.parseSubTreeList(support, rawConcepts, Concept.class, concept -> concept.setDataset(support.getDataset()));
		for (Concept<?> concept : concepts) {
			support.getNamespaceStorage().updateConcept(concept);
		}
	}
}
