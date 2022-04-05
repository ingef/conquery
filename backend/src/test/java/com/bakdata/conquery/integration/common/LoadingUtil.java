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
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
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
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@UtilityClass
public class LoadingUtil {

	public static void importPreviousQueries(StandaloneSupport support, RequiredData content, User user) throws IOException {
		// Load previous query results if available
		int id = 1;
		for (ResourceFile queryResults : content.getPreviousQueryResults()) {
			UUID queryId = new UUID(0L, id++);

			final CsvParser parser = support.getConfig().getCsv().withParseHeaders(false).withSkipHeader(false).createParser();
			String[][] data = parser.parseAll(queryResults.stream()).toArray(new String[0][]);

			ConceptQuery q = new ConceptQuery(new CQExternal(Arrays.asList("ID", "DATE_SET"), data));

			ManagedExecution<?> managed = support.getNamespace().getExecutionManager()
												 .createQuery(support.getNamespace().getNamespaces(), q, queryId, user, support.getNamespace().getDataset());

			user.addPermission(managed.createPermission(AbilitySets.QUERY_CREATOR));

			if (managed.getState() == ExecutionState.FAILED) {
				fail("Query failed");
			}
		}

		for (JsonNode queryNode : content.getPreviousQueries()) {
			ObjectMapper mapper = new SingletonNamespaceCollection(support.getNamespaceStorage().getCentralRegistry()).injectIntoNew(Jackson.MAPPER);
			mapper = support.getDataset().injectIntoNew(mapper);
			Query query = mapper.readerFor(Query.class).readValue(queryNode);
			UUID queryId = new UUID(0L, id++);

			ManagedExecution<?>
					managed =
					support.getNamespace()
						   .getExecutionManager()
						   .createQuery(support.getNamespace().getNamespaces(), query, queryId, user, support.getNamespace().getDataset());
			user.addPermission(ExecutionPermission.onInstance(AbilitySets.QUERY_CREATOR, managed.getId()));

			if (managed.getState() == ExecutionState.FAILED) {
				fail("Query failed");
			}
		}

		// wait only if we actually did anything
		if (!content.getPreviousQueryResults().isEmpty()) {
			support.waitUntilWorkDone();
		}
	}

	public static void importTables(StandaloneSupport support, List<RequiredTable> tables) throws JSONException {

		for (RequiredTable rTable : tables) {
			final Table table = rTable.toTable(support.getDataset(), support.getNamespace().getStorage().getCentralRegistry());
			support.getDatasetsProcessor().addTable(table, support.getNamespace());
		}
	}

	public static void importTableContents(StandaloneSupport support, RequiredTable[] tables) throws Exception {
		importTableContents(support, Arrays.asList(tables));
	}

	public static List<File> generateCqpp(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
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
				input.setPrimary(rTable.getPrimaryColumn().createOutput());
				input.setSourceFile(rTable.getCsv().getName());
				input.setOutput(new OutputDescription[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = rTable.getColumns()[i].createOutput();
				}
			}
			desc.setInputs(new TableInputDescriptor[]{input});

			Jackson.MAPPER.writeValue(descriptionFile, desc);

			descriptions.add(descriptionFile);
			preprocessedFiles.add(outFile);
		}
		// preprocess
		support.preprocessTmp(support.getTmpDir(), descriptions);
		//clear the MDC location from the preprocessor
		ConqueryMDC.clearLocation();
		return preprocessedFiles;
	}

	public static void importCqppFile(StandaloneSupport support, File cqpp) {
		assertThat(cqpp).exists();

		final URI addImport = HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "addImport")
											 .queryParam("file", cqpp)
											 .buildFromMap(Map.of(ResourceConstants.DATASET, support.getDataset().getName()));

		final Response response = support.getClient()
										 .target(addImport)
										 .request(MediaType.APPLICATION_JSON)
										 .post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE));

		assertThat(response.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);
	}

	public static void updateCqppFile(StandaloneSupport support, File cqpp, Response.Status.Family expectedResponseFamily, String expectedReason) {
		assertThat(cqpp).exists();

		final URI addImport = HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "updateImport")
											 .queryParam("file", cqpp)
											 .buildFromMap(Map.of(
													 ResourceConstants.DATASET, support.getDataset().getId()
											 ));

		final Response response = support.getClient()
										 .target(addImport)
										 .request(MediaType.APPLICATION_JSON)
										 .put(Entity.entity(Entity.json(""), MediaType.APPLICATION_JSON_TYPE));

		assertThat(response.getStatusInfo().getFamily()).isEqualTo(expectedResponseFamily);
		assertThat(response.getStatusInfo().getReasonPhrase()).isEqualTo(expectedReason);
	}

	public static void importCqppFiles(StandaloneSupport support, List<File> cqppFiles) {
		for (File cqpp : cqppFiles) {
			importCqppFile(support, cqpp);
		}
	}

	public static void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		List<File> cqpps = generateCqpp(support, tables);
		importCqppFiles(support, cqpps);
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


	private static List<Concept<?>> getConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException {
		return ConqueryTestSpec.parseSubTreeList(
				support,
				rawConcepts,
				Concept.class,
				c -> c.setDataset(support.getDataset())
		);
	}

	public static void addConcepts(StandaloneSupport support, ArrayNode rawConcepts, @NonNull Response.Status.Family expectedResponseFamily)
			throws JSONException, IOException {
		List<Concept<?>> concepts = getConcepts(support, rawConcepts);
		for (Concept<?> concept : concepts) {
			addConcept(support, concept, expectedResponseFamily);
		}
	}

	public static void updateConcepts(StandaloneSupport support, ArrayNode rawConcepts, @NonNull Response.Status.Family expectedResponseFamily)
			throws JSONException, IOException {
		List<Concept<?>> concepts = getConcepts(support, rawConcepts);
		for (Concept<?> concept : concepts) {
			updateConcept(support, concept, expectedResponseFamily);
		}
	}

	private static void updateConcept(@NonNull StandaloneSupport support, @NonNull Concept<?> concept, @NonNull Response.Status.Family expectedResponseFamily) {
		final URI
				conceptURI =
				HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "updateConcept")
							   .buildFromMap(Map.of(
									   ResourceConstants.DATASET, support.getDataset().getId()
							   ));

		final Response response = support.getClient()
										 .target(conceptURI)
										 .request(MediaType.APPLICATION_JSON)
										 .put(Entity.entity(concept, MediaType.APPLICATION_JSON_TYPE));


		assertThat(response.getStatusInfo().getFamily()).isEqualTo(expectedResponseFamily);
	}

	private static void addConcept(@NonNull StandaloneSupport support, @NonNull Concept<?> concept, @NonNull Response.Status.Family expectedResponseFamily) {
		final URI
				conceptURI =
				HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "addConcept")
							   .buildFromMap(Map.of(
									   ResourceConstants.DATASET, support.getDataset().getId()
							   ));

		final Response response = support.getClient()
										 .target(conceptURI)
										 .request(MediaType.APPLICATION_JSON)
										 .post(Entity.entity(concept, MediaType.APPLICATION_JSON_TYPE));


		assertThat(response.getStatusInfo().getFamily()).isEqualTo(expectedResponseFamily);
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
