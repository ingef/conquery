package com.bakdata.conquery.integration.json;

import static com.bakdata.conquery.integration.common.LoadingUtil.*;

import java.util.Collections;

import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class WorkerTestDataImporter implements TestDataImporter {

	@Override
	public void importQueryTestData(StandaloneSupport support, QueryTest test) throws Exception {

		RequiredData content = test.getContent();

		importSecondaryIds(support, content.getSecondaryIds());
		importInternToExternMappers(support, test.getInternToExternMappings());

		waitUntilDone(support, () -> importSearchIndexes(support, test.getSearchIndexes()));
		waitUntilDone(support, () -> importTables(support, content.getTables(), content.isAutoConcept()));
		waitUntilDone(support, () -> importConcepts(support, test.getRawConcepts()));
		waitUntilDone(support, () -> importTableContents(support, content.getTables()));
		waitUntilDone(support, () -> importIdMapping(support, content));
		waitUntilDone(support, () -> importPreviousQueries(support, content, support.getTestUser()));
		waitUntilDone(support, () -> updateMatchingStats(support));

		sendUpdateMatchingStatsMessage(support);
	}

	@Override
	public void importFormTestData(StandaloneSupport support, FormTest test) throws Exception {

		RequiredData content = test.getContent();

		waitUntilDone(support, () -> importSecondaryIds(support, content.getSecondaryIds()));
		waitUntilDone(support, () -> importTables(support, content.getTables(), content.isAutoConcept()));
		waitUntilDone(support, () -> importConcepts(support, test.getRawConcepts()));
		waitUntilDone(support, () -> importTableContents(support, content.getTables()));
		waitUntilDone(support, () -> importIdMapping(support, content));
		waitUntilDone(support, () -> importPreviousQueries(support, content, support.getTestUser()));
	}

	@Override
	public void importFilterTestData(StandaloneSupport support, FilterTest test) throws Exception {

		RequiredData content = test.getContent();

		importInternToExternMappers(support, test.getInternToExternMappings());
		importSearchIndexes(support, test.getSearchIndices());
		waitUntilDone(support, () -> importTables(support, content.getTables(), content.isAutoConcept()));

		test.setConnector(ConqueryTestSpec.parseSubTree(
								  support,
								  test.getRawConnector(),
								  ConceptTreeConnector.class,
								  conn -> conn.setConcept(test.getConcept())
						  )
		);
		test.getConcept().setConnectors(Collections.singletonList((ConceptTreeConnector) test.getConnector()));

		waitUntilDone(support, () -> uploadConcept(support, support.getDataset(), test.getConcept()));
		waitUntilDone(support, () -> importTableContents(support, content.getTables()));
		waitUntilDone(support, () -> updateMatchingStats(support));
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
