package com.bakdata.conquery.models.datasets.concepts.filters;

import java.io.IOException;

import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.models.datasets.Dataset;
import com.github.powerlibraries.io.In;
import org.junit.jupiter.api.Test;

public class CompoundFilterTest {

	@Test
	public void testFEFilterCreation() throws IOException {
		final Dataset dataset = new Dataset("test");

		final String testJson = In.resource("/tests/filter/COMPOUND/COMPOUND.test.json").withUTF8().readAll();

		final FilterTest test = (FilterTest) JsonIntegrationTest.readJson(dataset, testJson);


	}

}
