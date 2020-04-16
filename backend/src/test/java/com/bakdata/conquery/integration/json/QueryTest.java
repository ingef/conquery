package com.bakdata.conquery.integration.json;

import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.RequiredColumn;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

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
		importIdMapping(support, content.getIdMapping());
		importPreviousQueries(support);
	}

	public static void importIdMapping(StandaloneSupport support, ResourceFile idMapping) throws JSONException, IOException {
		if(idMapping == null) {
			return;
		}
		try(InputStream in = idMapping.stream()) {
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

			ConceptQuery q = new ConceptQuery(new CQExternal(Arrays.asList(CQExternal.FormatColumn.ID, CQExternal.FormatColumn.DATE_SET), data));

			ManagedExecution<?> managed = ExecutionManager.runQuery(support.getNamespace().getNamespaces(), q, IntegrationUtils.getDefaultUser().getId(), support.getDataset().getId());
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

		ConqueryConfig.getInstance().setAdditionalFormats(ArrayUtils.EMPTY_STRING_ARRAY);
		List<File> preprocessedFiles = new ArrayList<>();

		for (RequiredTable rTable : tables) {
			//copy csv to tmp folder
			String name = rTable.getCsv().getName().substring(0, rTable.getCsv().getName().lastIndexOf('.'));
			FileUtils.copyInputStreamToFile(rTable.getCsv().stream(), new File(support.getTmpDir(), rTable.getCsv().getName()));

			//create import descriptor
			InputFile inputFile = InputFile.fromName(support.getConfig().getPreprocessor().getDirectories()[0], name, null);
			TableImportDescriptor desc = new TableImportDescriptor();
			desc.setInputFile(inputFile);
			desc.setName(rTable.getName() + "_import");
			desc.setTable(rTable.getName());
			TableInputDescriptor input = new TableInputDescriptor();
			{
				input.setPrimary(copyOutput(rTable.getPrimaryColumn()));
				input.setSourceFile(new File(inputFile.getCsvDirectory(), rTable.getCsv().getName()));
				input.setOutput(new OutputDescription[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = copyOutput(rTable.getColumns()[i]);
				}
			}
			desc.setInputs(new TableInputDescriptor[]{input});
			Jackson.MAPPER.writeValue(inputFile.getDescriptionFile(), desc);
			preprocessedFiles.add(inputFile.getPreprocessedFile());
		}
		//preprocess
		support.preprocessTmp();

		//import preprocessedFiles
		for (File file : preprocessedFiles) {
			support.getDatasetsProcessor().addImport(dataset, file);
		}

		IntegrationUtils.importIdMapping(support, content);
		IntegrationUtils.importPreviousQueries(support, content);
	}

	public static OutputDescription copyOutput(RequiredColumn column) {
		CopyOutput out = new CopyOutput();
		out.setInputColumn(column.getName());
		out.setInputType(column.getType());
		out.setName(column.getName());
		return out;
	}

}
