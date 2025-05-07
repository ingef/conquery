package com.bakdata.conquery.integration.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.csv.CsvRenderer;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
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
	public void executeTest(StandaloneSupport support) throws Exception {


		final ManagedExecutionId managedExecutionId = IntegrationUtils.assertQueryResult(support, form, -1, ExecutionState.DONE, support.getTestUser(), 201);

		log.info("{} QUERIES EXECUTED", getLabel());

		checkResults(support, (ManagedInternalForm<?>) support.getMetaStorage().getExecution(managedExecutionId), support.getTestUser());
	}

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {
		support.getTestImporter().importFormTestData(support, this);
		log.info("{} PARSE JSON FORM DESCRIPTION", getLabel());
		form = parseForm(support);
	}

	private Form parseForm(StandaloneSupport support) throws IOException {
		return parseSubTree(support, rawForm, Form.class, true);
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
				new PrintSettings(false, Locale.ENGLISH, standaloneSupport.getNamespace(), config, idPrinter::createId, null);

		checkSingleResult(managedForm, config, printSettings);

	}

	/**
	 * The form produces only one result, so the result is directly requested.
	 */
	private <F extends ManagedForm<?> & SingleTableResult> void checkSingleResult(F managedForm, ConqueryConfig config, PrintSettings printSettings)
			throws IOException {


		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			final CsvWriter writer = config.getCsv().createWriter(output);
			final CsvRenderer renderer = new CsvRenderer(writer, printSettings);

			renderer.toCSV(
					config.getIdColumns().getIdResultInfos(),
					managedForm.getResultInfos(),
					managedForm.streamResults(OptionalLong.empty()), printSettings, StandardCharsets.UTF_8
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
}
