package com.bakdata.conquery.integration.common;

import static com.bakdata.conquery.ConqueryConstants.EXTENSION_PREPROCESSED;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

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
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetsResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.univocity.parsers.csv.CsvParser;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.assertj.core.description.LazyTextDescription;

@Slf4j
@UtilityClass
public class LoadingUtil {

	public static void importDataset(Client client, UriBuilder adminUriBuilder, Dataset dataset) {

		final URI uri = HierarchyHelper.hierarchicalPath(adminUriBuilder, AdminDatasetsResource.class, "addDataset")
									   .build();

		final Invocation.Builder request = client.target(uri).request(MediaType.APPLICATION_JSON_TYPE);
		try (final Response response = request.post(Entity.json(dataset))) {

			assertThat(response.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);
		}
	}

	public static void importPreviousQueries(StandaloneSupport support, RequiredData content, User user) throws IOException {
		// Load previous query results if available
		int id = 1;
		for (ResourceFile queryResults : content.getPreviousQueryResults()) {
			UUID queryId = new UUID(0L, id++);

			final CsvParser parser = support.getConfig().getCsv().withParseHeaders(false).withSkipHeader(false).createParser();
			String[][] data = parser.parseAll(queryResults.stream()).toArray(new String[0][]);

			ConceptQuery query = new ConceptQuery(new CQExternal(Arrays.asList("ID", "DATE_SET"), data, false));

			ExecutionManager executionManager = support.getNamespace().getExecutionManager();
			ManagedExecution managed = executionManager.createExecution(query, queryId, user.getId(), support.getNamespace(), false);

			user.addPermission(managed.createPermission(AbilitySets.QUERY_CREATOR));

			if (managed.getState() == ExecutionState.FAILED) {
				fail("Query failed");
			}
		}

		for (JsonNode queryNode : content.getPreviousQueries()) {

			Query query = ConqueryTestSpec.parseSubTree(support, queryNode, Query.class, true);

			// Since we don't submit the query but injecting it into the manager we need to set the id resolver
			UUID queryId = new UUID(0L, id++);

			ExecutionManager executionManager = support.getNamespace().getExecutionManager();
			ManagedExecution managed = executionManager.createExecution(query, queryId, user.getId(), support.getNamespace(), false);

			user.addPermission(ExecutionPermission.onInstance(AbilitySets.QUERY_CREATOR, managed.getId()));

			if (managed.getState() == ExecutionState.FAILED) {
				fail("Query failed");
			}
		}
	}

	public static void importTables(StandaloneSupport support, List<RequiredTable> tables, boolean autoConcept) throws JSONException {

		for (RequiredTable rTable : tables) {
			final Table table = rTable.toTable(support.getDataset(), support.getDatasetRegistry());
			uploadTable(support, table);

			if (autoConcept) {
				final TreeConcept concept = AutoConceptUtil.createConcept(table);

				uploadConcept(support, table.getDataset(), concept);
			}
		}
	}

	private static void uploadTable(StandaloneSupport support, Table table) {
		final URI uri = HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "addTable")
									   .buildFromMap(Map.of(ResourceConstants.DATASET, support.getDataset()));

		final Invocation.Builder request = support.getClient().target(uri).request(MediaType.APPLICATION_JSON_TYPE);
		try (final Response response = request.post(Entity.json(table))) {

			assertThat(response.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);
		}
	}

	public static void uploadConcept(StandaloneSupport support, DatasetId dataset, Concept<?> concept) {
		final URI uri = HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "addConcept")
									   .buildFromMap(Map.of(ResourceConstants.DATASET, dataset.toString()));

		final Invocation.Builder request = support.getClient().target(uri).request(MediaType.APPLICATION_JSON_TYPE);
		try (final Response response = request.post(Entity.json(concept))) {

			assertThat(response.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);
		}
	}

	public static void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables) throws Exception {
		List<File> cqpps = generateCqpp(support, tables);

		importCqppFiles(support, cqpps);
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

	public static void importCqppFiles(StandaloneSupport support, List<File> cqppFiles) {
		for (File cqpp : cqppFiles) {
			uploadCqpp(support, cqpp, false, Response.Status.Family.SUCCESSFUL);
		}

		support.waitUntilWorkDone();


	}

	public static void uploadCqpp(StandaloneSupport support, File cqpp, boolean update, Response.Status.Family expectedResponseFamily) {
		if(update) {
			assertThat(cqpp).exists();
		}

		final String methodName = update ? "updateCqppImport" : "uploadImport";

		final URI addImport =
				HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, methodName)
							   .buildFromMap(Map.of(ResourceConstants.DATASET, support.getDataset()));

		final Entity<FileInputStream> entity;
		try {
			entity = Entity.entity(new FileInputStream(cqpp), MediaType.APPLICATION_OCTET_STREAM);


			final Invocation.Builder request = support.getClient()
													  .target(addImport)
													  .request(MediaType.APPLICATION_JSON);

			final Invocation invocation = update ? request.buildPut(entity) : request.buildPost(entity);

			log.info("sending CQPP with {}", invocation);

			try (final Response response = invocation.invoke()) {

				assertThat(response.getStatusInfo().getFamily())
						.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
						.isEqualTo(expectedResponseFamily);
			}
		}
		catch (FileNotFoundException e) {
			fail("Cqpp not found", e);
		}
	}

	public static void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException {

		List<Concept<?>> concepts = ConqueryTestSpec.parseSubTreeList(
				support,
				rawConcepts,
				Concept.class,
				c -> {}
		);

		for (Concept<?> concept : concepts) {
			uploadConcept(support, support.getDataset(), concept);
		}
	}

	public static void updateConcepts(StandaloneSupport support, ArrayNode rawConcepts, @NonNull Response.Status.Family expectedResponseFamily)
			throws IOException {
		List<Concept<?>> concepts = getConcepts(support, rawConcepts);
		for (Concept<?> concept : concepts) {
			updateConcept(support, concept, expectedResponseFamily);
		}


	}

	private static List<Concept<?>> getConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws IOException {
		return ConqueryTestSpec.parseSubTreeList(
				support,
				rawConcepts,
				Concept.class,
				c -> {}
		);
	}

	private static void updateConcept(@NonNull StandaloneSupport support, @NonNull Concept<?> concept, @NonNull Response.Status.Family expectedResponseFamily) {
		final URI
				conceptURI =
				HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "updateConcept")
							   .buildFromMap(Map.of(
									   ResourceConstants.DATASET, support.getDataset()
							   ));

		final Invocation.Builder request = support.getClient()
												  .target(conceptURI)
												  .request(MediaType.APPLICATION_JSON);
		try (final Response response = request
				.put(Entity.entity(concept, MediaType.APPLICATION_JSON_TYPE))) {

			assertThat(response.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
					.isEqualTo(expectedResponseFamily);
		}
	}

	public static void importIdMapping(StandaloneSupport support, RequiredData content) throws IOException {
		if (content.getIdMapping() == null) {
			return;
		}

		try (InputStream in = content.getIdMapping().stream()) {
			support.getAdminDatasetsProcessor().setIdMapping(in, support.getNamespace());
		}
	}

	public static Map<String, SecondaryIdDescription> importSecondaryIds(StandaloneSupport support, List<RequiredSecondaryId> secondaryIds) {
		Map<String, SecondaryIdDescription> out = new HashMap<>();

		for (RequiredSecondaryId required : secondaryIds) {
			final SecondaryIdDescription description =
					required.toSecondaryId(support.getDataset());

			uploadSecondaryId(support, description);

			out.put(description.getName(), description);
		}

		return out;
	}

	private static void uploadSecondaryId(@NonNull StandaloneSupport support, @NonNull SecondaryIdDescription secondaryIdDescription) {
		final URI
				conceptURI =
				HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "addSecondaryId")
							   .buildFromMap(Map.of(
									   ResourceConstants.DATASET, support.getDataset()
							   ));

		final Invocation.Builder request = support.getClient()
												  .target(conceptURI)
												  .request(MediaType.APPLICATION_JSON);
		try (final Response response = request
				.post(Entity.entity(secondaryIdDescription, MediaType.APPLICATION_JSON_TYPE))) {


			assertThat(response.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);
		}
	}

	public static void importInternToExternMappers(StandaloneSupport support, List<InternToExternMapper> internToExternMappers) {
		for (InternToExternMapper internToExternMapper : internToExternMappers) {
			uploadInternalToExternalMappings(support, internToExternMapper);
		}
	}

	private static void uploadInternalToExternalMappings(@NonNull StandaloneSupport support, @NonNull InternToExternMapper mapping) {
		final URI
				conceptURI =
				HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "addInternToExternMapping")
							   .buildFromMap(Map.of(
									   ResourceConstants.DATASET, support.getDataset()
							   ));

		final Invocation.Builder request = support.getClient()
												  .target(conceptURI)
												  .request(MediaType.APPLICATION_JSON);
		try (final Response response = request
				.post(Entity.entity(mapping, MediaType.APPLICATION_JSON_TYPE))) {


			assertThat(response.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);
		}
	}

	public static void importSearchIndexes(StandaloneSupport support, List<SearchIndex> searchIndexes) {
		for (SearchIndex internToExternMapper : searchIndexes) {
			uploadSearchIndex(support, internToExternMapper);
		}
	}

	private static void uploadSearchIndex(@NonNull StandaloneSupport support, @NonNull SearchIndex searchIndex) {
		final URI
				conceptURI =
				HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "addSearchIndex")
							   .buildFromMap(Map.of(
									   ResourceConstants.DATASET, support.getDataset()
							   ));

		Invocation.Builder request = support.getClient()
											.target(conceptURI)
											.request(MediaType.APPLICATION_JSON);
		try(final Response response = request.post(Entity.entity(searchIndex, MediaType.APPLICATION_JSON_TYPE))) {
			assertThat(response.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);
		}
	}

	public static void updateMatchingStats(@NonNull StandaloneSupport support) {
		final URI matchingStatsUri = HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder()
															, AdminDatasetResource.class, "postprocessNamespace")
													.buildFromMap(Map.of(DATASET, support.getDataset()));

		final Response post = support.getClient().target(matchingStatsUri)
									 .request(MediaType.APPLICATION_JSON_TYPE)
									 .post(null);
		post.close();
	}

}
