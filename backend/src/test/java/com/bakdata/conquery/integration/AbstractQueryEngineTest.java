package com.bakdata.conquery.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryStatus;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.powerlibraries.io.In;

public abstract class AbstractQueryEngineTest implements ConqueryTestSpec {

	@JsonIgnore
	protected abstract IQuery getQuery();

	protected abstract File getExpectedCsv();


	@Override
	public void executeTest(StandaloneSupport standaloneSupport) throws IOException, JSONException {
		IQuery query = getQuery();

		standaloneSupport.waitUntilWorkDone();

		ManagedQuery managed = standaloneSupport.getNamespace().getQueryManager().createQuery(query);

		managed.awaitDone(1, TimeUnit.DAYS);

		if (managed.getStatus() == QueryStatus.FAILED) {
			fail("Query failed");
		}

		List<String> actual = managed.toCSV(standaloneSupport.getCfg()).collect(Collectors.toList());

		File expectedCsv = getExpectedCsv();

		List<String> expected = In.file(expectedCsv).readLines();


		assertThat(actual)
				.as("Results for %s are not as expected.", this)
				.containsExactlyInAnyOrderElementsOf(expected);

		log.info(
				"INTEGRATION TEST SUCCESSFUL {} {} on {} rows",
				getClass().getSimpleName(),
				this,
				expected.size()
		);
	}
}
