package com.bakdata.conquery.integration.json.filter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.integration.common.LoadingUtil;
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
	public void importRequiredData(StandaloneSupport support) throws Exception {

		((ObjectNode) rawContent.get("tables")).put("name", "table");

		content = parseSubTree(support, rawContent, RequiredData.class);

		importTables(support);
		support.waitUntilWorkDone();


		importConcepts(support);
		support.waitUntilWorkDone();
		
		query = parseQuery(support);

		LoadingUtil.importTableContents(support, content.getTables(), support.getDataset());
	}



	private void importConcepts(StandaloneSupport support) throws JSONException, IOException, ConfigurationException {
		Dataset dataset = support.getDataset();

		concept = new VirtualConcept();
		concept.setLabel("concept");

		concept.setDataset(support.getDataset());

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

		cqTable.setFilters(Collections.singletonList(result));
		cqTable.setConnector(connector);

		CQConcept cqConcept = new CQConcept();

		cqTable.setConcept(cqConcept);

		cqConcept.setElements(Collections.singletonList(concept));
		cqConcept.setTables(Collections.singletonList(cqTable));

		if (dateRange != null) {
			CQDateRestriction restriction = new CQDateRestriction();
			restriction.setDateRange(dateRange);
			restriction.setChild(cqConcept);
			return new ConceptQuery(restriction);
		}
		return  new ConceptQuery(cqConcept);
	}

	@Override
	public IQuery getQuery() {
		return query;
	}

	private void importTables(StandaloneSupport support) throws JSONException {
		Dataset dataset = support.getDataset();

		for (RequiredTable rTable : content.getTables()) {
			support.getDatasetsProcessor().addTable(rTable.toTable(support.getDataset(), support.getNamespace().getStorage().getCentralRegistry()), support.getNamespace());
		}
	}
}
