package com.bakdata.conquery.integration.query;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.integration.AbstractQueryEngineTest;
import com.bakdata.conquery.integration.ConqueryTestSpec;
import com.bakdata.conquery.integration.common.RequiredColumn;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.validators.ExistingFile;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.bakdata.conquery.models.preproc.Input;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.models.query.IQuery;
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

	@NotEmpty
	private String label;
	@ExistingFile
	private File expectedCsv;

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

		// Load previous query results if available
		/*
		File queryResults = content.getPreviousQueryResults();
		if(queryResults != null) {
			if(queryResults.exists()) {
				con.createStatement().executeUpdate(
						"COPY "+this.getSchema()+".query_results FROM '" + queryResults.getAbsolutePath()+ "' DELIMITER ',' CSV HEADER");
			} else {
				throw new IOException("The File "+ queryResults.getAbsolutePath() + " does not exsist.");
			}
		}*/
	}

	private void importTableContents(StandaloneSupport support) throws IOException, JSONException {
		CsvParserSettings settings = new CsvParserSettings();
		CsvFormat format = new CsvFormat();
		format.setLineSeparator("\n");
		settings.setFormat(format);
		settings.setHeaderExtractionEnabled(true);
		DateFormats.initialize(new String[0]);
		List<File> preprocessedFiles = new ArrayList<>();

		for (RequiredTable rTable : content.getTables()) {
			//copy csv to tmp folder
			String name = rTable.getCsv().getName().substring(0, rTable.getCsv().getName().lastIndexOf('.'));
			FileUtils.copyFileToDirectory(rTable.getCsv(), support.getTmpDir());

			//create import descriptor
			InputFile inputFile = InputFile.fromName(support.getCfg().getPreprocessor().getDirectories()[0], name);
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
				list -> list.forEach(c -> c.setDataset(support.getDataset()))
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

	@Override
	public String toString() {
		return label;
	}
}
