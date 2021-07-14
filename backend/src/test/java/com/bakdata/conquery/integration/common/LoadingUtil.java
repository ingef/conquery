package com.bakdata.conquery.integration.common;

import static com.bakdata.conquery.ConqueryConstants.EXTENSION_PREPROCESSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.concept.specific.CQExternal;
import com.bakdata.conquery.apiv1.query.concept.specific.CQExternal.FormatColumn;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQExternal;
import com.bakdata.conquery.apiv1.query.concept.specific.CQExternal.FormatColumn;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.univocity.parsers.csv.CsvParser;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@UtilityClass
public class LoadingUtil {

	public static void importPreviousQueries(StandaloneSupport support, RequiredData content) throws IOException {
		importPreviousQueries(support, content, support.getTestUser());
	}

	public static void importPreviousQueries(StandaloneSupport support, RequiredData content, User user) throws IOException {
		// Load previous query results if available
		int id = 1;
		for (ResourceFile queryResults : content.getPreviousQueryResults()) {
			UUID queryId = new UUID(0L, id++);

			final CsvParser parser = support.getConfig().getCsv().withParseHeaders(false).withSkipHeader(false).createParser();
			String[][] data = parser.parseAll(queryResults.stream()).toArray(new String[0][]);

			ConceptQuery q = new ConceptQuery(new CQExternal(Arrays.asList(FormatColumn.ID, FormatColumn.DATE_SET), data));

			ManagedExecution<?> managed = support.getNamespace().getExecutionManager().createQuery(support.getNamespace().getNamespaces(),q, queryId, user, support.getNamespace().getDataset());
			user.addPermission(support.getMetaStorage(), ExecutionPermission.onInstance(AbilitySets.QUERY_CREATOR, managed.getId()));

			if (managed.getState() == ExecutionState.FAILED) {
				fail("Query failed");
			}
		}

		for (JsonNode queryNode : content.getPreviousQueries()) {
			ObjectMapper mapper = new SingletonNamespaceCollection(support.getNamespaceStorage().getCentralRegistry()).injectInto(Jackson.MAPPER);
			mapper = support.getDataset().injectInto(mapper);
			Query query = mapper.readerFor(Query.class).readValue(queryNode);
			UUID queryId = new UUID(0L, id++);

			ManagedExecution<?> managed = support.getNamespace().getExecutionManager().createQuery(support.getNamespace().getNamespaces(),query, queryId, user, support.getNamespace().getDataset());
			user.addPermission(support.getMetaStorage(), ExecutionPermission.onInstance(AbilitySets.QUERY_CREATOR, managed.getId()));

			if (managed.getState() == ExecutionState.FAILED) {
				fail("Query failed");
			}
		}

		// wait only if we actually did anything
		if (!content.getPreviousQueryResults().isEmpty()) {
			support.waitUntilWorkDone();
		}
	}

	public static void importTables(StandaloneSupport support, RequiredData content) throws JSONException {

		for (RequiredTable rTable : content.getTables()) {
			support.getDatasetsProcessor().addTable(rTable.toTable(support.getDataset(), support.getNamespace().getStorage().getCentralRegistry()), support.getNamespace());
		}
	}
	
	public static void importTableContents(StandaloneSupport support, RequiredTable[] tables, Dataset dataset) throws Exception {
		importTableContents(support, Arrays.asList(tables), dataset);
	}
	
	public static void importTableContents(StandaloneSupport support, RequiredTable[] tables) throws Exception {
		importTableContents(support, Arrays.asList(tables), support.getDataset());
	}
	
	public static void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		importTableContents(support, tables, support.getDataset());
	}
	
	public static void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables, Dataset dataset) throws Exception {
		List<File> preprocessedFiles = new ArrayList<>();
		List<File> descriptions = new ArrayList<>();

		for (RequiredTable rTable : tables) {
			// copy csv to tmp folder
			String name = rTable.getName();
			FileUtils.copyInputStreamToFile(rTable.getCsv().stream(), new File(support.getTmpDir(), rTable.getCsv().getName()));

			// create import descriptor
			final File descriptionFile = support.getTmpDir().toPath().resolve(name + ConqueryConstants.EXTENSION_DESCRIPTION).toFile();
			final File outFile = support.getTmpDir().toPath().resolve(name + EXTENSION_PREPROCESSED).toFile();

			TableImportDescriptor desc = new TableImportDescriptor();

			desc.setName(name);
			desc.setTable(name);
			TableInputDescriptor input = new TableInputDescriptor();
			{
				input.setPrimary(IntegrationUtils.copyOutput(rTable.getPrimaryColumn()));
				input.setSourceFile(rTable.getCsv().getName());
				input.setOutput(new OutputDescription[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = IntegrationUtils.copyOutput(rTable.getColumns()[i]);
				}
			}
			desc.setInputs(new TableInputDescriptor[] { input });

			Jackson.MAPPER.writeValue(descriptionFile, desc);

			descriptions.add(descriptionFile);
			preprocessedFiles.add(outFile);
		}
		// preprocess
		support.preprocessTmp(support.getTmpDir(), descriptions);
		//clear the MDC location from the preprocessor
		ConqueryMDC.clearLocation();

		// import preprocessedFiles
		for (File file : preprocessedFiles) {
			assertThat(file).exists();

			final URI addImport = HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "addImport")
												 .queryParam("file", file)
												 .buildFromMap(Map.of(ResourceConstants.DATASET, support.getDataset().getName()));

			final Response response = support.getClient()
											 .target(addImport)
											 .request(MediaType.APPLICATION_JSON)
											 .post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE));

			assertThat(response.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);
		}
	}

	public static void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException {
		Dataset dataset = support.getDataset();

		List<Concept<?>> concepts = ConqueryTestSpec.parseSubTreeList(
			support,
			rawConcepts,
			Concept.class,
			c -> c.setDataset(support.getDataset())
		);

		for (Concept<?> concept : concepts) {

			support.getDatasetsProcessor().addConcept(dataset, concept);
		}
	}
	

	public static void importIdMapping(StandaloneSupport support, RequiredData content) throws JSONException, IOException {
		if (content.getIdMapping() == null) {
			return;
		}

		try (InputStream in = content.getIdMapping().stream()) {
			support.getDatasetsProcessor().setIdMapping(in, support.getNamespace());
		}
	}

	public static Map<String, SecondaryIdDescription> importSecondaryIds(StandaloneSupport support, List<RequiredSecondaryId> secondaryIds) {
		Map<String, SecondaryIdDescription> out = new HashMap<>();

		for (RequiredSecondaryId required : secondaryIds) {
			final SecondaryIdDescription description = required.toSecondaryId();
			support.getDatasetsProcessor()
				   .addSecondaryId(support.getNamespace(), description);

			out.put(description.getName(), description);
		}

		return out;
	}
}
