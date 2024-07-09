package com.bakdata.conquery.integration.json;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
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

	@NotNull
	private List<InternToExternMapper> internToExternMappings = List.of();
	@NotNull
	private List<SearchIndex> searchIndexes = List.of();

	@JsonIgnore
	private Query query;
	@Override
	public Query getQuery() {
		return query;
	}

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {
		support.getTestImporter().importQueryTestData(support, this);
		query = IntegrationUtils.parseQuery(support, rawQuery);
	}

}
