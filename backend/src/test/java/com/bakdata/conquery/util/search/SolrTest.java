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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

		// Cleanup core
		solrConfig.getSolrClient().deleteByQuery("*:*");
	}

	@Test
	@Order(0)
	public void addData() throws SolrServerException, IOException {
		Column column = setupSearchable();
		searchProcessor.registerValues(column, List.of("a", "b"));

	}

	private static @NotNull Column setupSearchable() {
		Column column = new Column();
		column.setName("column1");
		Table table = new Table();
		table.setName("table");
		table.setDataset(new DatasetId("dataset"));
		column.setTable(table);
		return column;
	}

	@Test
	@Order(1)
	public void queryData() throws SolrServerException, IOException {
		searchProcessor.
	}



	@AfterAll
	public static void afterAll() throws SolrServerException, IOException {
		// Cleanup core
		solrConfig.getSolrClient().deleteByQuery("*:*");
	}
}
