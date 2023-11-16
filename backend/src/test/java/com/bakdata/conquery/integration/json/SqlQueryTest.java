package com.bakdata.conquery.integration.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

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
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.powerlibraries.io.In;
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

	@Override
	protected void compareResults(Response csvResponse, SingleTableResult executionResult, ManagedExecution execution) throws IOException {

		// we will not compare the first line containing the column names because the execution service is not using the printer yet,
		// and probably we don't want to use the generated sql column names as result csv column names!
		List<String> actual = In.stream(((InputStream) csvResponse.getEntity())).readLines();
		actual.remove(0);

		ResourceFile expectedCsv = getExpectedCsv();

		List<String> expected = In.stream(expectedCsv.stream()).readLines();
		expected.remove(0);

		assertThat(actual).as("Results for %s are not as expected.", this)
						  .containsExactlyInAnyOrderElementsOf(expected);

		log.info("INTEGRATION TEST SUCCESSFUL {} {} on {} rows", getClass().getSimpleName(), this, expected.size());
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
