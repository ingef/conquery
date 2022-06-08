package com.bakdata.conquery.integration.json.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.integration.json.AbstractQueryEngineTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
@Setter
@CPSType(id = "FILTER_TEST", base = ConqueryTestSpec.class)
public class FilterTest extends AbstractQueryEngineTest {

	private ResourceFile expectedCsv;

	@NotNull
	@JsonProperty("filterValue")
	private ObjectNode rawFilterValue;

	@NotNull
	@JsonProperty("content")
	private ObjectNode rawContent;

	private FEFilter expectedFrontendConfig;

	@JsonIgnore
	private RequiredData content;


	@NotNull
	@JsonProperty("connector")
	private ObjectNode rawConnector;

	private Range<LocalDate> dateRange;

	@JsonIgnore
	private Query query;

	@JsonIgnore
	private Connector connector;
	private TreeConcept concept;

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {

		((ObjectNode) rawContent.get("tables")).put("name", "table");

		content = parseSubTree(support, rawContent, RequiredData.class);

		LoadingUtil.importTables(support, content.getTables());
		support.waitUntilWorkDone();


		importConcepts(support);
		support.waitUntilWorkDone();

		query = parseQuery(support);

		LoadingUtil.importTableContents(support, content.getTables());
	}


	private void importConcepts(StandaloneSupport support) throws JSONException, IOException {
		Dataset dataset = support.getDataset();

		concept = new TreeConcept();
		concept.setLabel("concept");
		concept.setValidator(support.getValidator());

		concept.setDataset(support.getDataset());

		rawConnector.put("name", "connector");
		rawConnector.put("table", "table");

		((ObjectNode) rawConnector.get("filters")).put("name", "filter");

		connector = parseSubTree(
				support,
				rawConnector,
				ConceptTreeConnector.class,
				conn -> conn.setConcept(concept)
		);

		concept.setConnectors(Collections.singletonList((ConceptTreeConnector) connector));
		LoadingUtil.uploadConcept(support, dataset, concept);
	}

	private Query parseQuery(StandaloneSupport support) throws JSONException, IOException {
		final String filterId = support.getDataset().getName() + ".concept.connector.filter";
		rawFilterValue.put("filter", filterId);

		if (expectedFrontendConfig != null) {
			expectedFrontendConfig.setId(FilterId.Parser.INSTANCE.parse(filterId));
		}


		FilterValue<?> result = parseSubTree(support, rawFilterValue, Jackson.MAPPER.getTypeFactory().constructType(FilterValue.class));

		CQTable cqTable = new CQTable();

		cqTable.setFilters(Collections.singletonList(result));
		cqTable.setConnector(connector);

		CQConcept cqConcept = new CQConcept();

		cqTable.setConcept(cqConcept);

		cqConcept.setElements(Collections.singletonList(concept));
		cqConcept.setTables(Collections.singletonList(cqTable));

		if (dateRange != null) {
			CQDateRestriction restriction = new CQDateRestriction(dateRange, cqConcept);
			return new ConceptQuery(restriction);
		}
		return new ConceptQuery(cqConcept);
	}

	@Override
	public Query getQuery() {
		return query;
	}

	@Override
	public void executeTest(StandaloneSupport standaloneSupport) throws IOException {
		try {
			final FEFilter actual = connector.getFilters().iterator().next().createFrontendConfig();

			if (expectedFrontendConfig != null) {
				assertThat(actual).usingRecursiveComparison().isEqualTo(expectedFrontendConfig);
			}
		}
		catch (ConceptConfigurationException e) {
			throw new IllegalStateException(e);
		}

		super.executeTest(standaloneSupport);
	}
}
