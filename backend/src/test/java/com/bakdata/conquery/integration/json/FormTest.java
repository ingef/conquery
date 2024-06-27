package com.bakdata.conquery.integration.json;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.csv.CsvRenderer;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.IdColumnUtil;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvWriter;
import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

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
		support.getTestImporter().importFormTestData(support, this);
		log.info("{} PARSE JSON FORM DESCRIPTION", getLabel());
		form = parseForm(support);
	}

	@Override
	public void executeTest(StandaloneSupport support) throws Exception {
		Namespace namespace = support.getNamespace();

		//		assertThat(support.getValidator().validate(form))
		//				.describedAs("Form Validation Errors")
		//				.isEmpty();


		final ManagedExecutionId managedExecutionId = IntegrationUtils.assertQueryResult(support, form, -1, ExecutionState.DONE, support.getTestUser(), 201);

		//		ManagedInternalForm<? > managedForm = (ManagedInternalForm<?>) support
		//				.getNamespace()
		//				.getExecutionManager()
		//				.runQuery(namespace, form, support.getTestUser(), support.getConfig(), false);
		//
		//		managedForm.awaitDone(10, TimeUnit.MINUTES);
		//		if (managedForm.getState() != ExecutionState.DONE) {
		//			if (managedForm.getState() == ExecutionState.FAILED) {
		//				fail(getLabel() + " Query failed");
		//			}
		//			else {
		//				fail(getLabel() + " not finished after 10 min");
		//			}
		//		}

		log.info("{} QUERIES EXECUTED", getLabel());

		checkResults(support, (ManagedInternalForm<?>) support.getMetaStorage().getExecution(managedExecutionId), support.getTestUser());
	}

	private void checkResults(StandaloneSupport standaloneSupport, ManagedInternalForm<?> managedForm, User user) throws IOException {

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
						standaloneSupport.getNamespace(),
						config,
						idPrinter::createId
				);

		checkSingleResult(managedForm, config, printSettings);

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
						   .flatMap(managedQuery -> managedQuery.streamResults(OptionalLong.empty(), printSettings.getNamespace().getExecutionManager()))
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
	private <F extends ManagedForm<?> & SingleTableResult> void checkSingleResult(F managedForm, ConqueryConfig config, PrintSettings printSettings)
			throws IOException {


		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			final CsvWriter writer = config.getCsv().createWriter(output);
			final CsvRenderer renderer =
					new CsvRenderer(writer, printSettings);

			renderer.toCSV(
					config.getIdColumns().getIdResultInfos(),
					managedForm.getResultInfos(),
					managedForm.streamResults(OptionalLong.empty(), printSettings.getNamespace().getExecutionManager())
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


	private Form parseForm(StandaloneSupport support) throws JSONException, IOException {
		return parseSubTree(support, rawForm, Form.class, true);
	}
}
