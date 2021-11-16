package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;

public class PostalCodeFilterTest implements ProgrammaticIntegrationTest{
	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		final StandaloneSupport conquery = testConquery.getSupport(name);
		MetaStorage storage = conquery.getMetaStorage();

		String testJson = In.resource("/tests/query/POSTAL_CODE_FILTER_TESTS/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final Namespace namespace = conquery.getNamespace();
	}
}
