package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.CopyOutput;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

@UtilityClass
public class IntegrationUtils {

	public static User getDefaultUser() {
		return ConqueryConfig.getInstance().getAuthorization().getInitialUsers().get(0).getUser();
	}


	/**
	 * Load the constellation of roles, users and permissions into the provided storage.
	 */
	public static void importPermissionConstellation(MasterMetaStorage storage, Role[] roles,  RequiredUser[] rUsers) throws JSONException {

		for (Role role : roles) {
			storage.addRole(role);
		}

		for (RequiredUser rUser : rUsers) {
			User user = rUser.getUser();
			storage.addUser(user);

			RoleId[] rolesInjected = rUser.getRolesInjected();

			for (RoleId mandatorId : rolesInjected) {
				user.addRole(storage, storage.getRole(mandatorId));
			}
		}
	}


	public static void clearAuthStorage(MasterMetaStorage storage, Role[] roles, RequiredUser[] rUsers) {
		// Clear MasterStorage
		for (Role mandator : roles) {
			storage.removeRole(mandator.getId());
		}
		for (RequiredUser rUser : rUsers) {
			storage.removeUser(rUser.getUser().getId());
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

	public static void importPreviousQueries(StandaloneSupport support, RequiredData content) throws JSONException, IOException {
		Namespaces namespaces = support.getNamespace().getNamespaces();
		UserId userId = support.getTestUser().getId();
		DatasetId dataset = support.getNamespace().getDataset().getId();

		// Load previous query results if available
		int id = 1;
		for (ResourceFile queryResults : content.getPreviousQueryResults()) {
			UUID queryId = new UUID(0L, id++);

			//Just read the file without parsing headers etc.
			CsvParserSettings parserSettings = support.getConfig().getCsv()
													  .withParseHeaders(false)
													  .withSkipHeader(false)
													  .createCsvParserSettings();

			CsvParser parser = new CsvParser(parserSettings);

			String[][] data = parser.parseAll(queryResults.stream()).toArray(String[][]::new);

			ConceptQuery query = new ConceptQuery(new CQExternal(Arrays.asList(CQExternal.FormatColumn.ID, CQExternal.FormatColumn.DATE_SET), data))
										 .resolve(new QueryResolveContext(dataset, support.getNamespace().getNamespaces()));

			ManagedExecution<?> managed = ExecutionManager.runQuery( namespaces, query, queryId, userId, dataset);
			managed.awaitDone(1, TimeUnit.DAYS);

			if (managed.getState() == ExecutionState.FAILED) {
				fail("Query failed");
			}
		}

		//wait only if we actually did anything
		if (!content.getPreviousQueryResults().isEmpty()) {
			support.waitUntilWorkDone();
		}
	}

	public static void importTableContents(StandaloneSupport support, Collection<RequiredTable> tables, Dataset dataset) throws IOException, JSONException {

		List<File> preprocessedFiles = new ArrayList<>();

		for (RequiredTable rTable : tables) {
			//copy csv to tmp folder
			String name = rTable.getCsv().getName().substring(0, rTable.getCsv().getName().lastIndexOf('.'));
			FileUtils.copyInputStreamToFile(rTable.getCsv().stream(), new File(support.getTmpDir(), rTable.getCsv().getName()));

			//create import descriptor
			InputFile inputFile = InputFile.fromName(support.getConfig().getPreprocessor().getDirectories()[0], name, null);
			TableImportDescriptor desc = new TableImportDescriptor();
			desc.setInputFile(inputFile);
			desc.setName(rTable.getName() + "_import");
			desc.setTable(rTable.getName());
			TableInputDescriptor input = new TableInputDescriptor();
			{
				input.setPrimary(copyOutput(rTable.getPrimaryColumn()));
				input.setSourceFile(new File(inputFile.getCsvDirectory(), rTable.getCsv().getName()));
				input.setOutput(new OutputDescription[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = copyOutput(rTable.getColumns()[i]);
				}
			}
			desc.setInputs(new TableInputDescriptor[]{input});
			Jackson.MAPPER.writeValue(inputFile.getDescriptionFile(), desc);
			preprocessedFiles.add(inputFile.getPreprocessedFile());
		}
		//preprocess
		support.preprocessTmp();

		//import preprocessedFiles
		for (File file : preprocessedFiles) {
			support.getDatasetsProcessor().addImport(dataset, file);
		}
	}

	public static OutputDescription copyOutput(RequiredColumn column) {
		CopyOutput out = new CopyOutput();
		out.setInputColumn(column.getName());
		out.setInputType(column.getType());
		out.setName(column.getName());
		return out;
	}

	public static void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException, ConfigurationException {
		Dataset dataset = support.getDataset();

		List<Concept<?>> concepts = ConqueryTestSpec.parseSubTree(
				support,
				rawConcepts,
				Jackson.MAPPER.getTypeFactory().constructParametricType(List.class, Concept.class),
				list -> list.forEach(c -> c.setDataset(support.getDataset().getId()))
		);

		for (Concept<?> concept : concepts) {
			support.getDatasetsProcessor().addConcept(dataset, concept);
		}
	}

	public static IQuery parseQuery(StandaloneSupport support, JsonNode rawQuery) throws JSONException, IOException {
		return ConqueryTestSpec.parseSubTree(support, rawQuery, IQuery.class);
	}

	public static void importTables(StandaloneSupport support, RequiredData content) throws JSONException {
		Dataset dataset = support.getDataset();

		for (RequiredTable rTable : content.getTables()) {
			support.getDatasetsProcessor().addTable(dataset, rTable.toTable());
		}
	}
}
