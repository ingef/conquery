package com.bakdata.conquery.integration.json.filter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.integration.json.AbstractQueryEngineTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.virtual.VirtualConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConceptConnector;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;


@Slf4j @Getter @Setter
@CPSType(id = "FILTER_TEST", base = ConqueryTestSpec.class)
public class FilterTest extends AbstractQueryEngineTest {

	private ResourceFile expectedCsv;

	@NotNull
	@JsonProperty("filterValue")
	private ObjectNode rawFilterValue;

	@NotNull
	@JsonProperty("content")
	private ObjectNode rawContent;

	@JsonIgnore
	private RequiredData content;


	@NotNull
	@JsonProperty("connector")
	private ObjectNode rawConnector;

	private Range<LocalDate> dateRange;

	@JsonIgnore
	private IQuery query;

	@JsonIgnore
	private Connector connector;
	private VirtualConcept concept;

	@Override
	public void importRequiredData(StandaloneSupport support) throws IOException, JSONException, ConfigurationException {

		((ObjectNode) rawContent.get("tables")).put("name", "table");

		content = parseSubTree(support, rawContent, RequiredData.class);

		importTables(support);
		support.waitUntilWorkDone();

		importConcepts(support);
		support.waitUntilWorkDone();
		
		query = parseQuery(support);

		importTableContents(support);
	}

	private void importTableContents(StandaloneSupport support) throws IOException, JSONException {

		List<File> preprocessedFiles = new ArrayList<>();

		for (RequiredTable rTable : content.getTables()) {
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
				input.setPrimary(IntegrationUtils.copyOutput(rTable.getPrimaryColumn()));
				input.setSourceFile(new File(inputFile.getCsvDirectory(), rTable.getCsv().getName()));
				input.setOutput(new OutputDescription[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = IntegrationUtils.copyOutput(rTable.getColumns()[i]);
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
			support.getDatasetsProcessor().addImport(support.getDataset(), file);
		}
	}

	private void importConcepts(StandaloneSupport support) throws JSONException, IOException, ConfigurationException {
		Dataset dataset = support.getDataset();

		concept = new VirtualConcept();
		concept.setLabel("concept");

		concept.setDataset(support.getDataset().getId());

		rawConnector.put("name", "connector");
		rawConnector.put("table", "table");

		((ObjectNode) rawConnector.get("filter")).put("name", "filter");

		connector = parseSubTree(
				support,
				rawConnector,
				VirtualConceptConnector.class,
				conn -> conn.setConcept(concept)
		);

		concept.setConnectors(Collections.singletonList((VirtualConceptConnector) connector));
		support.getDatasetsProcessor().addConcept(dataset, concept);
	}

	private IQuery parseQuery(StandaloneSupport support) throws JSONException, IOException {
		rawFilterValue.put("filter", support.getDataset().getName() + ".concept.connector.filter");


		FilterValue<?> result = parseSubTree(support, rawFilterValue, Jackson.MAPPER.getTypeFactory().constructType(FilterValue.class));

		CQTable cqTable = new CQTable();

		cqTable.setResolvedConnector(connector);
		cqTable.setFilters(Collections.singletonList(result));
		cqTable.setId(connector.getId());

		CQConcept cqConcept = new CQConcept();

		cqTable.setConcept(cqConcept);

		cqConcept.setIds(Collections.singletonList(concept.getId()));
		cqConcept.setTables(Collections.singletonList(cqTable));

		if (dateRange != null) {
			CQDateRestriction restriction = new CQDateRestriction();
			restriction.setDateRange(dateRange);
			restriction.setChild(cqConcept);
			return new ConceptQuery(restriction);
		}
		else {
			return  new ConceptQuery(cqConcept);
		}
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
