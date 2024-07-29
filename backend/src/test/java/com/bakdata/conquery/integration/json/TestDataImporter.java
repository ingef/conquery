package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredSecondaryId;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.node.ArrayNode;

public interface TestDataImporter {

	void importQueryTestData(StandaloneSupport support, QueryTest queryTest) throws Exception;

	void importFormTestData(StandaloneSupport support, FormTest formTest) throws Exception;

	void importFilterTestData(StandaloneSupport support, FilterTest filterTest) throws Exception;

	void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception;

	default void importSearchIndexes(StandaloneSupport support, List<SearchIndex> searchIndices) {
		waitUntilDone(support, () -> LoadingUtil.importSearchIndexes(support, searchIndices));
	}

	default void importIdMapping(StandaloneSupport support, RequiredData content) {
		waitUntilDone(support, () -> LoadingUtil.importIdMapping(support, content));
	}

	default void importSecondaryIds(StandaloneSupport support, List<RequiredSecondaryId> secondaryIds) {
		waitUntilDone(support, () -> LoadingUtil.importSecondaryIds(support, secondaryIds));
	}

	default void importTables(StandaloneSupport support, List<RequiredTable> tables, boolean autoConcept) throws JSONException {
		waitUntilDone(support, () -> LoadingUtil.importTables(support, tables, autoConcept));
	}

	default void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException {
		waitUntilDone(support, () -> LoadingUtil.importConcepts(support, rawConcepts));
	}

	default void importPreviousQueries(StandaloneSupport support, RequiredData content) {
		waitUntilDone(support, () -> LoadingUtil.importPreviousQueries(support, content, support.getTestUser()));
	}

	default void waitUntilDone(StandaloneSupport support, CheckedRunnable<?> runnable) {
		runnable.run();
		support.waitUntilWorkDone();
	}

	@FunctionalInterface
	interface CheckedRunnable<E extends Exception> extends Runnable {

		@Override
		default void run() throws RuntimeException {
			try {
				runThrows();
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		void runThrows() throws E;

	}

}
