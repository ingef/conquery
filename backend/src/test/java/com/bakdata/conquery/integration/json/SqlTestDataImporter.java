package com.bakdata.conquery.integration.json;

import static com.bakdata.conquery.integration.common.LoadingUtil.importInternToExternMappers;

import java.util.Collection;
import java.util.Collections;

import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
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
	}

	@Override
	public void importFilterTestData(StandaloneSupport support, FilterTest test) throws Exception {
		RequiredData content = test.getContent();

		importInternToExternMappers(support, test.getInternToExternMappings());
		importSearchIndexes(support, test.getSearchIndices());
		importTables(support, content.getTables(), content.isAutoConcept());

		test.setConnector(ConqueryTestSpec.parseSubTree(
								  support,
								  test.getRawConnector(),
								  ConceptTreeConnector.class,
								  conn -> {
									  conn.setTable(new TableId(support.getDataset().getDataset(), FilterTest.TABLE_NAME));
									  conn.setConcept(test.getConcept());
								  },
								  true
						  )
		);
		test.getConcept().setConnectors(Collections.singletonList((ConceptTreeConnector) test.getConnector()));

		waitUntilDone(support, () -> LoadingUtil.uploadConcept(support, support.getDataset(), test.getConcept()));
		importTableContents(support, content.getTables());

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
