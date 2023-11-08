package com.bakdata.conquery.integration.sql;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.result.csv.CsvRenderer;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.conquery.SqlManagedQuery;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.util.io.IdColumnUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;

@Getter
@Setter
@CPSType(id = "SQL_TEST", base = ConqueryTestSpec.class)
@Slf4j
public class SqlIntegrationTestSpec extends ConqueryTestSpec<SqlStandaloneSupport> {

	private static final String EXPECTED_SQL_FILENAME = "expected.sql";

	private List<Dialect> supportedDialects;

	@NotNull
	@JsonProperty("query")
	private JsonNode rawQuery;

	@JsonIgnore
	private String description;

	@NotNull
	private String expectedCsv;

	@Valid
	@NotNull
	private RequiredData content;

	@NotNull
	@JsonProperty("concepts")
	private ArrayNode rawConcepts;

	@JsonIgnore
	private Query query;

	@JsonIgnore
	private Path specDir;

	/**
	 * @return True if the test specification contains the dialect, or if no allowed dialects are specified,
	 * 	which considers the spec to be allowed for all dialects.
	 */
	public boolean supportsDialects(Dialect dialect) {
		return getSupportedDialects() == null || getSupportedDialects().contains(dialect);
	}

	@SneakyThrows
	public static SqlIntegrationTestSpec fromJsonSpec(Path path) {
		SqlIntegrationTestSpec test = readSpecFromJson(path);
		test.setSpecDir(path.getParent());
		return test;
	}

	@Override
	public void executeTest(SqlStandaloneSupport support) throws IOException {
		for (RequiredTable table : content.getTables()) {
			support.getTableImporter().importTableIntoDatabase(table);
		}

		User user = support.getTestUser();
		ConqueryConfig config = support.getConfig();
		Namespace namespace = support.getNamespace();
		SqlManagedQuery managedQuery = support.getExecutionManager().runQuery(namespace, getQuery(), user, support.getDataset(), config, false);

		final IdPrinter idPrinter = IdColumnUtil.getIdPrinter(user, managedQuery, namespace, config.getIdColumns().getIds());
		final Locale locale = I18n.LOCALE.get();
		final PrintSettings settings = new PrintSettings(true, locale, namespace, config, idPrinter::createId);

		StringWriter stringWriter = new StringWriter();
		CsvRenderer csvRenderer = new CsvRenderer(new CSVConfig().createWriter(stringWriter), settings);
		csvRenderer.toCSV(config.getIdColumns().getIdResultInfos(), managedQuery.getResultInfos(), managedQuery.streamResults());
		log.debug("Actual CSV: {}", stringWriter);

		SqlExecutionResult result = managedQuery.getResult();
		List<EntityResult> resultCsv = result.getTable();

		Path expectedCsvFile = this.specDir.resolve(this.expectedCsv);
		List<EntityResult> expectedCsv = support.getTableImporter().readExpectedEntities(expectedCsvFile);

		Assertions.assertThat(resultCsv)
				  .usingRecursiveFieldByFieldElementComparatorIgnoringFields("entityId")
				  .containsExactlyInAnyOrderElementsOf(expectedCsv);
	}

	@Override
	public void importRequiredData(SqlStandaloneSupport support) throws IOException, JSONException {
		importTables(support);
		importConcepts(support);
		Query parsedQuery = ConqueryTestSpec.parseSubTree(support, getRawQuery(), Query.class);
		setQuery(parsedQuery);
	}

	private static SqlIntegrationTestSpec readSpecFromJson(Path path) throws IOException {
		final ObjectReader objectReader = Jackson.MAPPER.readerFor(SqlIntegrationTestSpec.class);
		return objectReader.readValue(Files.readString(path));
	}

	private void importTables(SqlStandaloneSupport support) {
		for (RequiredTable rTable : getContent().getTables()) {
			final Table table = rTable.toTable(support.getDataset(), support.getNamespaceStorage().getCentralRegistry());
			support.getNamespaceStorage().addTable(table);
		}
	}

	private void importConcepts(SqlStandaloneSupport support) throws IOException, JSONException {
		List<Concept<?>>
				concepts =
				ConqueryTestSpec.parseSubTreeList(support, getRawConcepts(), Concept.class, concept -> concept.setDataset(support.getDataset()));

		for (Concept<?> concept : concepts) {
			support.getNamespaceStorage().updateConcept(concept);
		}
	}

}
