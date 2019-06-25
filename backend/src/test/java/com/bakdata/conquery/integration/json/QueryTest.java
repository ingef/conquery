package com.bakdata.conquery.integration.json;

import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.bakdata.conquery.integration.common.RequiredColumn;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.csv.CSV;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.DevAuthConfig;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.bakdata.conquery.models.preproc.Input;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.concept.specific.CQExternal.FormatColumn;
import com.bakdata.conquery.util.io.ConfigCloner;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParserSettings;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@CPSType(id = "QUERY_TEST", base = ConqueryTestSpec.class)
public class QueryTest extends AbstractQueryEngineTest {

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
	private IQuery query;

	@Override
	public void importRequiredData(StandaloneSupport support) throws IOException, JSONException, ConfigurationException {
		importTables(support);
		support.waitUntilWorkDone();

		importConcepts(support);
		support.waitUntilWorkDone();
		query = parseQuery(support);

		importTableContents(support);
		support.waitUntilWorkDone();
		importPreviousQueries(support);
	}

	private void importPreviousQueries(StandaloneSupport support) throws JSONException, IOException {
		// Load previous query results if available
		int id = 1;
		for(ResourceFile queryResults : content.getPreviousQueryResults()) {
			UUID queryId = new UUID(0L, id++);
			ConqueryConfig config = ConfigCloner.clone(support.getConfig());
			config.getCsv().setSkipHeader(false);
			String[][] data = CSV.streamContent(config.getCsv(), queryResults.stream())
				.toArray(String[][]::new);

			ConceptQuery q = new ConceptQuery();
			q.setRoot(new CQExternal(Arrays.asList(FormatColumn.ID, FormatColumn.DATE_SET), data));
			
			ManagedExecution managed = support.getNamespace().getQueryManager().createQuery(q, queryId, DevAuthConfig.USER);
			managed.awaitDone(1, TimeUnit.DAYS);

			if (managed.getState() == ExecutionState.FAILED) {
				fail("Query failed");
			}
		}

		//wait only if we actually did anything
		if(!content.getPreviousQueryResults().isEmpty()) {
			support.waitUntilWorkDone();
		}
	}

	private void importTableContents(StandaloneSupport support) throws IOException, JSONException {
		CsvParserSettings settings = new CsvParserSettings();
		CsvFormat format = new CsvFormat();
		format.setLineSeparator("\n");
		settings.setFormat(format);
		settings.setHeaderExtractionEnabled(true);
		DateFormats.initialize(ArrayUtils.EMPTY_STRING_ARRAY);
		List<File> preprocessedFiles = new ArrayList<>();

		for (RequiredTable rTable : content.getTables()) {
			//copy csv to tmp folder
			String name = rTable.getCsv().getName().substring(0, rTable.getCsv().getName().lastIndexOf('.'));
			FileUtils.copyInputStreamToFile(rTable.getCsv().stream(), new File(support.getTmpDir(), rTable.getCsv().getName()));

			//create import descriptor
			InputFile inputFile = InputFile.fromName(support.getConfig().getPreprocessor().getDirectories()[0], name);
			ImportDescriptor desc = new ImportDescriptor();
			desc.setInputFile(inputFile);
			desc.setName(rTable.getName() + "_import");
			desc.setTable(rTable.getName());
			Input input = new Input();
			{
				input.setPrimary(copyOutput(0, rTable.getPrimaryColumn()));
				input.setSourceFile(new File(inputFile.getCsvDirectory(), rTable.getCsv().getName()));
				input.setOutput(new Output[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = copyOutput(i + 1, rTable.getColumns()[i]);
				}
			}
			desc.setInputs(new Input[]{input});
			Jackson.MAPPER.writeValue(inputFile.getDescriptionFile(), desc);
			preprocessedFiles.add(inputFile.getPreprocessedFile());
		}
		//preprocess
		support.preprocessTmp();

		//import preprocessedFiles
		for (File file : preprocessedFiles) {
			support.getDatasetsProcessor().addImport(support.getDataset(), file);
		}
	}

	private Output copyOutput(int columnPosition, RequiredColumn column) {
		CopyOutput out = new CopyOutput();
		out.setInputColumn(columnPosition);
		out.setInputType(column.getType());
		out.setName(column.getName());
		return out;
	}

	private void importConcepts(StandaloneSupport support) throws JSONException, IOException, ConfigurationException {
		Dataset dataset = support.getDataset();

		List<Concept<?>> concepts = parseSubTree(
				support,
				rawConcepts,
				Jackson.MAPPER.getTypeFactory().constructParametricType(List.class, Concept.class),
				list -> list.forEach(c -> c.setDataset(support.getDataset().getId()))
		);

		for (Concept<?> concept : concepts) {
			support.getDatasetsProcessor().addConcept(dataset, concept);
		}
	}

	private IQuery parseQuery(StandaloneSupport support) throws JSONException, IOException {
		return parseSubTree(support, rawQuery, IQuery.class);
	}

	@Override
	public IQuery getQuery() {
		return query;
	}

	private void importTables(StandaloneSupport support) throws JSONException {
		Dataset dataset = support.getDataset();

		for (RequiredTable rTable : content.getTables()) {
			support.getDatasetsProcessor().addTable(dataset, rTable.toTable());
		}
	}
}
