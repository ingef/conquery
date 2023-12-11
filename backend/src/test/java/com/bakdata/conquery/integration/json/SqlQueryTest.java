package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
@CPSType(id = "SQL_QUERY_TEST", base = ConqueryTestSpec.class)
public class SqlQueryTest extends AbstractQueryEngineTest {

	List<Dialect> supportedDialects;

	String description;

	@NotNull
	private ResourceFile expectedCsv;

	@NotNull
	@JsonProperty("query")
	private JsonNode rawQuery;

	@Valid
	@NotNull
	private RequiredData content;

	@NotNull
	@JsonProperty("concepts")
	private ArrayNode rawConcepts;

	@JsonIgnore
	private Query query;

	@JsonIgnore
	private CsvTableImporter csvTableImporter;

	@Override
	public void importRequiredData(StandaloneSupport support) throws IOException, JSONException {

		importTables(support);
		importConcepts(support);

		Query parsedQuery = ConqueryTestSpec.parseSubTree(support, getRawQuery(), Query.class);
		setQuery(parsedQuery);

		for (RequiredTable table : content.getTables()) {
			this.csvTableImporter.importTableIntoDatabase(table);
		}
	}

	public void setTableImporter(CsvTableImporter csvTableImporter) {
		this.csvTableImporter = csvTableImporter;
	}

	private void importTables(StandaloneSupport support) {
		for (RequiredTable rTable : getContent().getTables()) {
			final Table table = rTable.toTable(support.getDataset(), support.getNamespaceStorage().getCentralRegistry());
			support.getNamespaceStorage().addTable(table);
		}
	}

	private void importConcepts(StandaloneSupport support) throws IOException, JSONException {
		List<Concept<?>>
				concepts =
				ConqueryTestSpec.parseSubTreeList(support, getRawConcepts(), Concept.class, concept -> concept.setDataset(support.getDataset()));
		for (Concept<?> concept : concepts) {
			support.getNamespaceStorage().updateConcept(concept);
		}
	}

}
