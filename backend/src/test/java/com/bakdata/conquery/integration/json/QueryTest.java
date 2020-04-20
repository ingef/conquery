package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.RequiredColumn;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
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
		importIdMapping(support, content.getIdMapping());
		IntegrationUtils.importPreviousQueries(support, content);
	}

	public static void importIdMapping(StandaloneSupport support, ResourceFile idMapping) throws JSONException, IOException {
		if(idMapping == null) {
			return;
		}
		try(InputStream in = idMapping.stream()) {
			support.getDatasetsProcessor().setIdMapping(in, support.getNamespace());
		}
	}

	public static OutputDescription copyOutput(RequiredColumn column) {
		CopyOutput out = new CopyOutput();
		out.setInputColumn(column.getName());
		out.setInputType(column.getType());
		out.setName(column.getName());
		return out;
	}

}
