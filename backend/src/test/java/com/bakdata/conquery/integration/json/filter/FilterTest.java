package com.bakdata.conquery.integration.json.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.integration.json.AbstractQueryEngineTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
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

	public static final String CONCEPT_LABEL = "concept";
	public static final String TABLE_NAME = "table";
	private ResourceFile expectedCsv;

	@NotNull
	private List<InternToExternMapper> internToExternMappings = List.of();

	@NotNull
	private List<SearchIndex> searchIndices = Collections.emptyList();

	@NotNull
	@JsonProperty("filterValue")
	private ObjectNode rawFilterValue;

	@NotNull
	@JsonProperty("content")
	private ObjectNode rawContent;

	private FrontendFilterConfiguration.Top expectedFrontendConfig;

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

	@JsonIgnore
	private TreeConcept concept;

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {

		((ObjectNode) rawContent.get("tables")).put("name", TABLE_NAME);

		content = parseSubTree(support, rawContent, RequiredData.class, false);

		concept = new TreeConcept();
		concept.setLabel(CONCEPT_LABEL);

		concept.setNamespacedStorageProvider(support.getNamespaceStorage());
		concept.init();

		rawConnector.put("name", "connector");
		rawConnector.put(TABLE_NAME, TABLE_NAME);

		((ObjectNode) rawConnector.get("filters")).put("name", "filter");

		support.getTestImporter().importFilterTestData(support, this);

		query = parseQuery(support);
	}


	private Query parseQuery(StandaloneSupport support) throws IOException {
		final String filterId = support.getDataset().getName() + ".concept.connector.filter";
		rawFilterValue.put("filter", filterId);

		if (expectedFrontendConfig != null) {
			expectedFrontendConfig.setId(FilterId.Parser.INSTANCE.parse(filterId));
		}


		FilterValue<?> result = parseSubTree(support, rawFilterValue, Jackson.MAPPER.getTypeFactory().constructType(FilterValue.class), true);

		CQTable cqTable = new CQTable();

		cqTable.setFilters(Collections.singletonList(result));
		cqTable.setConnector(connector.getId());

		CQConcept cqConcept = new CQConcept();

		cqTable.setConcept(cqConcept);

		cqConcept.setElements(Collections.singletonList(concept.getId()));
		cqConcept.setTables(Collections.singletonList(cqTable));

		if (dateRange != null) {
			CQDateRestriction restriction = new CQDateRestriction(dateRange, cqConcept);
			return new ConceptQuery(restriction);
		}
		return new ConceptQuery(cqConcept);
	}

	@Override
	public void executeTest(StandaloneSupport standaloneSupport) throws IOException {
		try {
			final Connector internalConnector = standaloneSupport.getNamespace().getStorage().getAllConcepts().findFirst().get().getConnectors().getFirst();
			final FrontendFilterConfiguration.Top actual = internalConnector.getFilters().iterator().next().createFrontendConfig(standaloneSupport.getConfig());

			if (expectedFrontendConfig != null) {
				log.info("Checking actual FrontendConfig: {}", actual);
				assertThat(actual).usingRecursiveComparison().ignoringFieldsOfTypes(SerializationTestUtil.TYPES_TO_IGNORE).isEqualTo(expectedFrontendConfig);
			}
		}
		catch (ConceptConfigurationException e) {
			throw new IllegalStateException(e);
		}

		super.executeTest(standaloneSupport);
	}

	@Override
	public Query getQuery() {
		return query;
	}
}
