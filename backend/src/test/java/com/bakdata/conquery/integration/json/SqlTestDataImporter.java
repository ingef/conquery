package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredSecondaryId;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SqlTestDataImporter implements TestDataImporter {

	private final CsvTableImporter csvTableImporter;

	@Override
	public void importQueryTestData(StandaloneSupport support, QueryTest test) throws Exception {
		RequiredData content = test.getContent();
		importTables(support, content.getTables(), true);
		importConcepts(support, test.getRawConcepts());
		importTableContents(support, content.getTables());
	}

	@Override
	public void importFormTestData(StandaloneSupport support, FormTest formTest) throws Exception {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public void importFilterTestData(StandaloneSupport support, FilterTest filterTest) throws Exception {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public void importSecondaryIds(StandaloneSupport support, List<RequiredSecondaryId> secondaryIds) {
		for (RequiredSecondaryId required : secondaryIds) {
			final SecondaryIdDescription description =
					required.toSecondaryId(support.getDataset(), support.getDatasetRegistry().findRegistry(support.getDataset().getId()));
			support.getDatasetsProcessor().addSecondaryId(support.getNamespace(), description);
		}
	}

	@Override
	public void importTables(StandaloneSupport support, List<RequiredTable> tables, boolean autoConcept) throws JSONException {
		for (RequiredTable requiredTable : tables) {
			final Table table = requiredTable.toTable(support.getDataset(), support.getNamespaceStorage().getCentralRegistry());
			support.getNamespaceStorage().addTable(table);
		}
	}

	@Override
	public void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException {
		List<Concept<?>> concepts =
				ConqueryTestSpec.parseSubTreeList(support, rawConcepts, Concept.class, concept -> concept.setDataset(support.getDataset()));
		for (Concept<?> concept : concepts) {
			support.getNamespaceStorage().updateConcept(concept);
		}
	}

	@Override
	public void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		for (RequiredTable table : tables) {
			csvTableImporter.importTableIntoDatabase(table);
		}
	}

}
