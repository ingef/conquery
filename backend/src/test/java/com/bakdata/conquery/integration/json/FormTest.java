package com.bakdata.conquery.integration.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.io.result.csv.QueryToCSVRenderer;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.powerlibraries.io.In;
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
	
	@NotEmpty @Valid
	private Map<String, ResourceFile> expectedCsv;

	@Valid
	@NotNull
	private RequiredData content;
	@NotNull
	@JsonProperty("concepts")
	private ArrayNode rawConcepts;

	@JsonIgnore
	private Form form;

	@JsonIgnore
	private IdMappingConfig idMappingConfig;

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {

		LoadingUtil.importTables(support, content);
		support.waitUntilWorkDone();
		log.info("{} IMPORT TABLES", getLabel());

		importConcepts(support);
		support.waitUntilWorkDone();
		log.info("{} IMPORT CONCEPTS", getLabel());

		LoadingUtil.importTableContents(support, content.getTables());
		support.waitUntilWorkDone();
		log.info("{} IMPORT TABLE CONTENTS", getLabel());
		LoadingUtil.importPreviousQueries(support, content, support.getTestUser());

		support.waitUntilWorkDone();

		log.info("{} PARSE JSON FORM DESCRIPTION", getLabel());
		form = parseForm(support);

		idMappingConfig = support.getConfig().getIdMapping();
	}

	@Override
	public void executeTest(StandaloneSupport support) throws Exception {
		DatasetRegistry namespaces = support.getNamespace().getNamespaces();
		UserId userId = support.getTestUser().getId();
		DatasetId dataset = support.getNamespace().getDataset().getId();
		
		log.info("{} FORM INIT", getLabel());
		form.resolve(new QueryResolveContext(dataset, namespaces));
		
		ManagedExecution<?> managedForm = ExecutionManager.runQuery( namespaces, form, userId, dataset);

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

		checkResults(support, (ManagedForm) managedForm, support.getTestUser());
	}

	private void checkResults(StandaloneSupport standaloneSupport, ManagedForm managedForm, User user) throws IOException {
		Map<String, List<ManagedQuery>> managedMapping = managedForm.getSubQueries();
		IdMappingState mappingState = idMappingConfig.initToExternal(user, managedForm);
		
		for (Map.Entry<String, List<ManagedQuery>> managed : managedMapping.entrySet()) {
			log.info("{} CSV TESTING: {}", getLabel(), managed.getKey());
			List<String> actual = QueryToCSVRenderer
				.toCSV(
					new PrintSettings(true,Locale.ENGLISH, standaloneSupport.getNamespace().getNamespaces()),
					managed.getValue(),
					cer -> ResultUtil.createId(standaloneSupport.getNamespace(), cer, idMappingConfig, mappingState))
				.collect(Collectors.toList());
			
			assertThat(actual)
				.as("Checking result "+managed.getKey())
				.containsExactlyInAnyOrderElementsOf(
					In.stream(expectedCsv.get(managed.getKey()).stream())
					.withUTF8()
					.readLines()
				);
		}
	}

	private void importConcepts(StandaloneSupport support) throws JSONException, IOException {
		Dataset dataset = support.getDataset();

		List<Concept<?>> concepts = parseSubTreeList(
			support,
			rawConcepts,
			Concept.class,
			c -> c.setDataset(support.getDataset().getId())
		);

		for (Concept<?> concept : concepts) {
			support.getDatasetsProcessor().addConcept(dataset, concept);
		}
	}
	

	private Form parseForm(StandaloneSupport support) throws JSONException, IOException {
		return parseSubTree(support, rawForm, Form.class);
	}
}
