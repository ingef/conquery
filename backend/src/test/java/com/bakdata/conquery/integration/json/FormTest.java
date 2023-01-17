package com.bakdata.conquery.integration.json;

import static com.bakdata.conquery.integration.common.LoadingUtil.importIdMapping;
import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.csv.CsvRenderer;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.io.IdColumnUtil;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvWriter;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@CPSType(id = "FORM_TEST", base = ConqueryTestSpec.class)
public class FormTest extends ConqueryTestSpec {

	/*
	 * parse form as json first, because it may contain namespaced ids, that can only be resolved after
	 * concepts and tables have been imported.
	 */
	@JsonProperty("form")
	@NotNull
	private JsonNode rawForm;

	@NotEmpty
	@Valid
	private Map<String, ResourceFile> expectedCsv;

	@Valid
	@NotNull
	private RequiredData content;

	@JsonProperty("concepts")
	private ArrayNode rawConcepts;

	@JsonIgnore
	private Form form;

	@ValidationMethod(message = "Form test defines no concepts. Neither explicit nor automatic concepts")
	public boolean isWithConcepts() {
		return rawConcepts != null || content.isAutoConcept();
	}

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {
		importSecondaryIds(support, content.getSecondaryIds());
		support.waitUntilWorkDone();

		LoadingUtil.importTables(support, content.getTables(), content.isAutoConcept());
		support.waitUntilWorkDone();
		log.info("{} IMPORT TABLES", getLabel());

		importConcepts(support, rawConcepts);
		support.waitUntilWorkDone();
		log.info("{} IMPORT CONCEPTS", getLabel());

		LoadingUtil.importTableContents(support, content.getTables());
		support.waitUntilWorkDone();

		importIdMapping(support, content);
		support.waitUntilWorkDone();

		log.info("{} IMPORT TABLE CONTENTS", getLabel());
		LoadingUtil.importPreviousQueries(support, content, support.getTestUser());

		support.waitUntilWorkDone();

		log.info("{} PARSE JSON FORM DESCRIPTION", getLabel());
		form = parseForm(support);

	}

	@Override
	public void executeTest(StandaloneSupport support) throws Exception {
		DatasetRegistry namespaces = support.getDatasetRegistry();

		assertThat(support.getValidator().validate(form))
				.describedAs("Form Validation Errors")
				.isEmpty();


		ManagedForm managedForm = (ManagedForm) support
				.getNamespace()
				.getExecutionManager()
				.runQuery(namespaces, form, support.getTestUser(), support.getDataset(), support.getConfig(), false);

		managedForm.awaitDone(10, TimeUnit.MINUTES);
		if (managedForm.getState() != ExecutionState.DONE) {
			if (managedForm.getState() == ExecutionState.FAILED) {
				fail(getLabel() + " Query failed");
			}
			else {
				fail(getLabel() + " not finished after 10 min");
			}
		}

		log.info("{} QUERIES EXECUTED", getLabel());

		checkResults(support, managedForm, support.getTestUser());
	}

	private void checkResults(StandaloneSupport standaloneSupport, ManagedForm managedForm, User user) throws IOException {
		Map<String, List<ManagedQuery>> managedMapping = managedForm.getSubQueries();

		IdPrinter idPrinter = IdColumnUtil.getIdPrinter(
				user,
				managedForm,
				standaloneSupport.getNamespace(),
				standaloneSupport.getConfig().getIdColumns().getIds()
		);

		final ConqueryConfig config = standaloneSupport.getConfig();
		PrintSettings
				printSettings =
				new PrintSettings(
						false,
						Locale.ENGLISH,
						standaloneSupport.getDatasetsProcessor().getDatasetRegistry(),
						config,
						idPrinter::createId
				);

		if (managedForm instanceof SingleTableResult) {
			checkSingleResult((ManagedForm & SingleTableResult) managedForm, config, printSettings);
		}
		else {
			checkMultipleResult(managedMapping, config, printSettings);
		}

	}

	/**
	 * Checks result of subqueries instead of form result.
	 *
	 * @see FormTest#checkSingleResult(ManagedForm, ConqueryConfig, PrintSettings)
	 */
	private void checkMultipleResult(Map<String, List<ManagedQuery>> managedMapping, ConqueryConfig config, PrintSettings printSettings) throws IOException {
		for (Map.Entry<String, List<ManagedQuery>> managed : managedMapping.entrySet()) {
			List<ResultInfo> resultInfos = managed.getValue().get(0).getResultInfos();
			log.info("{} CSV TESTING: {}", getLabel(), managed.getKey());

			ByteArrayOutputStream output = new ByteArrayOutputStream();

			final CsvWriter writer = config.getCsv().createWriter(output);

			CsvRenderer renderer =
					new CsvRenderer(writer, printSettings);

			renderer.toCSV(
					config.getIdColumns().getIdResultInfos(),
					resultInfos,
					managed.getValue()
						   .stream()
						   .flatMap(ManagedQuery::streamResults)
			);

			writer.close();
			output.close();

			assertThat(In.stream(new ByteArrayInputStream(output.toByteArray())).withUTF8().readLines())
					.as("Checking result " + managed.getKey())
					.containsExactlyInAnyOrderElementsOf(
							In.stream(expectedCsv.get(managed.getKey()).stream())
							  .withUTF8()
							  .readLines()
					);
		}
	}

	/**
	 * The form produces only one result, so the result is directly requested.
	 *
	 * @see FormTest#checkMultipleResult(Map, ConqueryConfig, PrintSettings)
	 */
	private <F extends ManagedForm & SingleTableResult> void checkSingleResult(F managedForm, ConqueryConfig config, PrintSettings printSettings)
			throws IOException {


		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			final CsvWriter writer = config.getCsv().createWriter(output);
			final CsvRenderer renderer =
					new CsvRenderer(writer, printSettings);

			renderer.toCSV(
					config.getIdColumns().getIdResultInfos(),
					managedForm.getResultInfos(),
					managedForm.streamResults()
			);
			writer.close();

			assertThat(In.stream(new ByteArrayInputStream(output.toByteArray())).withUTF8().readLines())
					.as("Checking result " + managedForm.getLabelWithoutAutoLabelSuffix())
					.containsExactlyInAnyOrderElementsOf(
							In.stream(expectedCsv.values().iterator().next().stream())
							  .withUTF8()
							  .readLines()
					);
		}


	}

	private static void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException {
		if (rawConcepts == null) {
			return;
		}

		Dataset dataset = support.getDataset();

		List<Concept<?>> concepts = parseSubTreeList(
				support,
				rawConcepts,
				Concept.class,
				c -> c.setDataset(support.getDataset())
		);

		for (Concept<?> concept : concepts) {
			LoadingUtil.uploadConcept(support, dataset, concept);
		}
	}


	private Form parseForm(StandaloneSupport support) throws JSONException, IOException {
		return parseSubTree(support, rawForm, Form.class);
	}
}
