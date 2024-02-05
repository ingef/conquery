package com.bakdata.conquery.integration.json;

import static com.bakdata.conquery.integration.common.LoadingUtil.importInternToExternMappers;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredSecondaryId;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class WorkerTestDataImporter implements TestDataImporter {

	@Override
	public void importQueryTestData(StandaloneSupport support, QueryTest test) throws Exception {

		RequiredData content = test.getContent();

		importSecondaryIds(support, content.getSecondaryIds());
		importInternToExternMappers(support, test.getInternToExternMappings());

		waitUntilDone(support, () -> LoadingUtil.importSearchIndexes(support, test.getSearchIndexes()));
		waitUntilDone(support, () -> LoadingUtil.importTables(support, content.getTables(), content.isAutoConcept()));
		waitUntilDone(support, () -> LoadingUtil.importConcepts(support, test.getRawConcepts()));
		waitUntilDone(support, () -> LoadingUtil.importTableContents(support, content.getTables()));
		waitUntilDone(support, () -> LoadingUtil.importIdMapping(support, content));
		waitUntilDone(support, () -> LoadingUtil.importPreviousQueries(support, content, support.getTestUser()));
		waitUntilDone(support, () -> LoadingUtil.updateMatchingStats(support));

		sendUpdateMatchingStatsMessage(support);
	}

	@Override
	public void importFormTestData(StandaloneSupport support, FormTest test) throws Exception {

		RequiredData content = test.getContent();

		waitUntilDone(support, () -> LoadingUtil.importSecondaryIds(support, content.getSecondaryIds()));
		waitUntilDone(support, () -> LoadingUtil.importTables(support, content.getTables(), content.isAutoConcept()));
		waitUntilDone(support, () -> LoadingUtil.importConcepts(support, test.getRawConcepts()));
		waitUntilDone(support, () -> LoadingUtil.importTableContents(support, content.getTables()));
		waitUntilDone(support, () -> LoadingUtil.importIdMapping(support, content));
		waitUntilDone(support, () -> LoadingUtil.importPreviousQueries(support, content, support.getTestUser()));
	}

	@Override
	public void importFilterTestData(StandaloneSupport support, FilterTest test) throws Exception {

		RequiredData content = test.getContent();

		LoadingUtil.importInternToExternMappers(support, test.getInternToExternMappings());
		LoadingUtil.importSearchIndexes(support, test.getSearchIndices());
		waitUntilDone(support, () -> LoadingUtil.importTables(support, content.getTables(), content.isAutoConcept()));

		test.setConnector(ConqueryTestSpec.parseSubTree(
								  support,
								  test.getRawConnector(),
								  ConceptTreeConnector.class,
								  conn -> conn.setConcept(test.getConcept())
						  )
		);
		test.getConcept().setConnectors(Collections.singletonList((ConceptTreeConnector) test.getConnector()));

		waitUntilDone(support, () -> LoadingUtil.uploadConcept(support, support.getDataset(), test.getConcept()));
		waitUntilDone(support, () -> LoadingUtil.importTableContents(support, content.getTables()));
		waitUntilDone(support, () -> LoadingUtil.updateMatchingStats(support));
	}

	@Override
	public void importSecondaryIds(StandaloneSupport support, List<RequiredSecondaryId> secondaryIds) {
		waitUntilDone(support, () -> LoadingUtil.importSecondaryIds(support, secondaryIds));
	}

	@Override
	public void importTables(StandaloneSupport support, List<RequiredTable> tables, boolean autoConcept) throws JSONException {
		waitUntilDone(support, () -> LoadingUtil.importTables(support, tables, autoConcept));
	}

	@Override
	public void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException {
		waitUntilDone(support, () -> LoadingUtil.importConcepts(support, rawConcepts));
	}

	@Override
	public void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		waitUntilDone(support, () -> LoadingUtil.importTableContents(support, tables));
	}

	private void waitUntilDone(StandaloneSupport support, CheckedRunnable<?> runnable) {
		runnable.run();
		support.waitUntilWorkDone();
	}

	private static void sendUpdateMatchingStatsMessage(StandaloneSupport support) {
		DistributedNamespace namespace = (DistributedNamespace) support.getNamespace();
		namespace.getWorkerHandler().sendToAll(new UpdateMatchingStatsMessage(support.getNamespace().getStorage().getAllConcepts()));
		support.waitUntilWorkDone();
	}

}
