package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.util.Arrays;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
	public IQuery getQuery() {
		return query;
	}

	@Override
	public void importRequiredData(StandaloneSupport support) throws IOException, JSONException, ConfigurationException {
		IntegrationUtils.importTables(support, content);
		support.waitUntilWorkDone();

		IntegrationUtils.importConcepts(support, rawConcepts);
		support.waitUntilWorkDone();
		query = IntegrationUtils.parseQuery(support, rawQuery);

		IntegrationUtils.importTableContents(support, Arrays.asList(content.getTables()), support.getDataset());
		support.waitUntilWorkDone();
		importIdMapping(support);
		importPreviousQueries(support);
	}

	public void importIdMapping(StandaloneSupport support) throws JSONException, IOException {
		if(content.getIdMapping() == null) {
			return;
		}
		try(InputStream in = content.getIdMapping().stream()) {
			support.getDatasetsProcessor().setIdMapping(in, support.getNamespace());
		}
	}
	public void importPreviousQueries(StandaloneSupport support) throws JSONException, IOException {
		// Load previous query results if available
		int id = 1;
		for(ResourceFile queryResults : content.getPreviousQueryResults()) {
			UUID queryId = new UUID(0L, id++);

			//Just read the file without parsing headers etc.
			CsvParserSettings parserSettings = support.getConfig().getCsv()
													  .withParseHeaders(false)
													  .withSkipHeader(false)
													  .createCsvParserSettings();

			CsvParser parser = new CsvParser(parserSettings);

			String[][] data = parser.parseAll(queryResults.stream()).toArray(String[][]::new);

			ConceptQuery q = new ConceptQuery();
			q.setRoot(new CQExternal(Arrays.asList(FormatColumn.ID, FormatColumn.DATE_SET), data));
			
			ManagedExecution managed = support.getNamespace().getQueryManager().runQuery(q, queryId, DevAuthConfig.USER);
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

	public void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables, Dataset dataset) throws IOException, JSONException {
		CsvParserSettings settings = new CsvParserSettings();
		CsvFormat format = new CsvFormat();
		format.setLineSeparator("\n");
		settings.setFormat(format);
		settings.setHeaderExtractionEnabled(true);
		DateFormats.initialize(ArrayUtils.EMPTY_STRING_ARRAY);
		List<File> preprocessedFiles = new ArrayList<>();

		for (RequiredTable rTable : tables) {
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
				input.setPrimary(copyOutput(rTable.getPrimaryColumn()));
				input.setSourceFile(new File(inputFile.getCsvDirectory(), rTable.getCsv().getName()));
				input.setOutput(new Output[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = copyOutput(rTable.getColumns()[i]);
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
			support.getDatasetsProcessor().addImport(dataset, file);
		}
	}

	public static Output copyOutput(RequiredColumn column) {
		CopyOutput out = new CopyOutput();
		out.setInputColumn(column.getName());
		out.setInputType(column.getType());
		out.setName(column.getName());
		return out;
		IntegrationUtils.importIdMapping(support, content);
		IntegrationUtils.importPreviousQueries(support, content);
	}

}
