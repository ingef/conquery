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

import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.preproc.InputFile;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.concept.specific.CQExternal.FormatColumn;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.support.StandaloneSupport;
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

			final CsvParser parser = new CsvParser(support.getConfig().getCsv().withParseHeaders(false).withSkipHeader(false).createCsvParserSettings());
			String[][] data = parser.parseAll(queryResults.stream()).toArray(new String[0][]);

			ConceptQuery q = new ConceptQuery(new CQExternal(Arrays.asList(FormatColumn.ID, FormatColumn.DATE_SET), data));

			ManagedExecution<?> managed = ExecutionManager.createQuery(support.getNamespace().getNamespaces(),q, queryId, user.getId(), support.getNamespace().getDataset().getId());
			user.addPermission(support.getMetaStorage(), QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, managed.getId()));

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
		Dataset dataset = support.getDataset();

		for (RequiredTable rTable : content.getTables()) {
			support.getDatasetsProcessor().addTable(dataset, rTable.toTable());
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

		for (RequiredTable rTable : tables) {
			// copy csv to tmp folder
			String name = rTable.getCsv().getName().substring(0, rTable.getCsv().getName().lastIndexOf('.'));
			FileUtils.copyInputStreamToFile(rTable.getCsv().stream(), new File(support.getTmpDir(), rTable.getCsv().getName()));

			// create import descriptor
			InputFile inputFile = InputFile.fromName(support.getConfig().getPreprocessor().getDirectories()[0], name, null);
			TableImportDescriptor desc = new TableImportDescriptor();
			desc.setInputFile(inputFile);
			desc.setName(rTable.getName() + "_import");
			desc.setTable(rTable.getName());
			TableInputDescriptor input = new TableInputDescriptor();
			{
				input.setPrimary(IntegrationUtils.copyOutput(rTable.getPrimaryColumn()));
				input.setSourceFile(new File(inputFile.getCsvDirectory(), rTable.getCsv().getName()));
				input.setOutput(new OutputDescription[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = IntegrationUtils.copyOutput(rTable.getColumns()[i]);
				}
			}
			desc.setInputs(new TableInputDescriptor[] { input });
			Jackson.MAPPER.writeValue(inputFile.getDescriptionFile(), desc);
			preprocessedFiles.add(inputFile.getPreprocessedFile());
		}
		// preprocess
		support.preprocessTmp();
		//clear the MDC location from the preprocessor
		ConqueryMDC.clearLocation();

		// import preprocessedFiles
		for (File file : preprocessedFiles) {
			support.getDatasetsProcessor().addImport(dataset, file);
		}
	}

	public static void importConcepts(StandaloneSupport support, ArrayNode rawConcepts) throws JSONException, IOException {
		Dataset dataset = support.getDataset();

		List<Concept<?>> concepts = ConqueryTestSpec.parseSubTreeList(
			support,
			rawConcepts,
			Concept.class,
			c -> c.setDataset(support.getDataset().getId())
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
}
