package com.bakdata.conquery.util.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.search.SolrConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.search.solr.SolrBundle;
import io.dropwizard.core.setup.Environment;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SolrTest {

	public static SolrConfig solrConfig;
	public static SearchProcessor searchProcessor;

	@BeforeAll
	public static void smokeTest() throws Exception {
		SolrBundle solrBundle = new SolrBundle();

		ConqueryConfig conqueryConfig = new ConqueryConfig();

		solrConfig = new SolrConfig("http://gtl-ekifdz02:8983/solr/core1", false, new HashMap<>(), "solr", "SolrRocks");
		conqueryConfig.setSearch(solrConfig);
		solrBundle.run(conqueryConfig, new Environment(SolrTest.class.getSimpleName()));
		searchProcessor = solrConfig.createSearchProcessor();
	}

	@Test
	public void addData() throws SolrServerException, IOException {
		Column column = new Column();
		column.setName("column1");
		Table table = new Table();
		table.setName("table");
		table.setDataset(new DatasetId("dataset"));
		column.setTable(table);
		searchProcessor.registerValues(column, List.of("a", "b"));

		// Cleanup core
//		solrConfig.getSolrClient().deleteByQuery("*:*");

	}
}
