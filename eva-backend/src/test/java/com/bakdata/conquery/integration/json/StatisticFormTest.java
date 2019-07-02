package com.bakdata.conquery.integration.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryToCSVRenderer;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.ConqueryEscape;
import com.bakdata.conquery.util.support.LoadingUtil;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestAuth;
import com.bakdata.eva.forms.common.StatisticForm;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.powerlibraries.io.In;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@CPSType(id = "STATSTIC_FORM_TEST", base = ConqueryTestSpec.class)
public class StatisticFormTest extends ConqueryTestSpec {

	@NotNull
	private StatisticForm form;

	private ResourceFile expectedCsv;
	private ResourceFile expectedDescription;

	@Valid
	@NotNull
	private RequiredData content;
	@NotNull
	@JsonProperty("concepts")
	private ArrayNode rawConcepts;

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {

		LoadingUtil.importTables(support, content);
		support.waitUntilWorkDone();

		importConcepts(support);
		support.waitUntilWorkDone();

		LoadingUtil.importTableContents(support, content);
		support.waitUntilWorkDone();
		LoadingUtil.importPreviousQueries(support, content, TestAuth.SuperUser.INSTANCE);

		MasterMetaStorage storage = support.getStandaloneCommand().getMaster().getStorage();
		form.init(storage.getNamespaces(), TestAuth.SuperUser.INSTANCE);

	}

	@Override
	public void executeTest(StandaloneSupport support) throws Exception {
		MasterMetaStorage storage = support.getStandaloneCommand().getMaster().getStorage();
		Namespaces namespaces = storage.getNamespaces();
		
		List<ManagedQuery> managed = form.executeQuery(support.getDataset(), TestAuth.SuperUser.INSTANCE, namespaces);
		
		for(ManagedQuery q : managed) {
			q.awaitDone(10, TimeUnit.MINUTES);

			if (q.getState() == ExecutionState.FAILED) {
				fail("Query failed");
			}
		}

		QueryToCSVRenderer renderer = new QueryToCSVRenderer(support.getNamespace());
		PrintSettings settings = PrintSettings.builder().prettyPrint(false).nameExtractor(form.getColumnNamer().getNamer()).build();
		List<String> actual = managed
			.stream()
			.flatMap(q -> renderer.toCSV(settings, q))
			.collect(Collectors.toList());
		for(String line : actual) {
			log.info(line);
		}
		
		// Prepare generated description
		String description = form.toStatisticJSON(namespaces);
		description = ConqueryEscape.unescape(description);
		JsonNode descriptionNode = Jackson.MAPPER.readTree(description);
		clearTimestamp(descriptionNode);
		
		// Prepare expected description
		String expectedDesc = injectDataset(support.getDataset().getId(), In.stream(expectedDescription.stream()).readAll());
		expectedDesc = ConqueryEscape.unescape(expectedDesc);
		JsonNode expectedDescNode = Jackson.MAPPER.readTree(expectedDesc);
		clearTimestamp(expectedDescNode);
		
		// If the following fails, do a diff on the outputs
		assertThat(descriptionNode).isEqualTo(expectedDescNode);
	}

	private void importConcepts(StandaloneSupport support) throws JSONException, IOException, ConfigurationException {
		Dataset dataset = support.getDataset();

		List<Concept<?>> concepts = parseSubTree(
			support,
			rawConcepts,
			Jackson.MAPPER.getTypeFactory().constructParametricType(List.class, Concept.class),
			list -> list.forEach(c -> c.setDataset(support.getDataset().getId())));

		for (Concept<?> concept : concepts) {
			support.getDatasetsProcessor().addConcept(dataset, concept);
		}
	}

	private void clearTimestamp(JsonNode descriptionTree) {
		((ObjectNode) descriptionTree).set("timestamp", null);
	}

	private String injectDataset(DatasetId dataset, String input) {
		input = StringUtils.replace(input, "${dataset}", dataset.toString());
		return input;
	}
}
