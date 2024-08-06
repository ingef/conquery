package com.bakdata.conquery.integration.json;

import static com.bakdata.conquery.integration.common.LoadingUtil.importInternToExternMappers;

import java.util.Collection;
import java.util.Collections;

import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class WorkerTestDataImporter implements TestDataImporter {

	@Override
	public void importQueryTestData(StandaloneSupport support, QueryTest test) throws Exception {

		RequiredData content = test.getContent();

		importSecondaryIds(support, content.getSecondaryIds());
		importInternToExternMappers(support, test.getInternToExternMappings());

		importSearchIndexes(support, test.getSearchIndexes());
		importTables(support, content.getTables(), content.isAutoConcept());
		importConcepts(support, test.getRawConcepts());
		waitUntilDone(support, () -> LoadingUtil.importTableContents(support, content.getTables()));
		importIdMapping(support, content);
		importPreviousQueries(support, content);
		waitUntilDone(support, () -> LoadingUtil.updateMatchingStats(support));
	}

	@Override
	public void importFormTestData(StandaloneSupport support, FormTest test) throws Exception {

		RequiredData content = test.getContent();

		importSecondaryIds(support, content.getSecondaryIds());
		importTables(support, content.getTables(), content.isAutoConcept());
		importConcepts(support, test.getRawConcepts());
		waitUntilDone(support, () -> LoadingUtil.importTableContents(support, content.getTables()));
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
				conn -> conn.setConcept(test.getConcept())
		));
		test.getConcept().setConnectors(Collections.singletonList((ConceptTreeConnector) test.getConnector()));

		waitUntilDone(support, () -> LoadingUtil.uploadConcept(support, support.getDataset(), test.getConcept()));
		waitUntilDone(support, () -> LoadingUtil.importTableContents(support, content.getTables()));
		waitUntilDone(support, () -> LoadingUtil.updateMatchingStats(support));
	}


	@Override
	public void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		waitUntilDone(support, () -> LoadingUtil.importTableContents(support, tables));
	}

}