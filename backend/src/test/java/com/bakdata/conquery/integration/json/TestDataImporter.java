package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.integration.common.RequiredSecondaryId;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.node.ArrayNode;

public interface TestDataImporter {

	void importQueryTestData(StandaloneSupport support, QueryTest queryTest) throws Exception;

	void importFormTestData(StandaloneSupport support, FormTest formTest) throws Exception;

	void importFilterTestData(StandaloneSupport support, FilterTest filterTest) throws Exception;

	void importSecondaryIds(StandaloneSupport support, List<RequiredSecondaryId> secondaryIds);

	void importTables(StandaloneSupport support, List<RequiredTable> tables, boolean autoConcept) throws JSONException;

	void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException;

	void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception;

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
