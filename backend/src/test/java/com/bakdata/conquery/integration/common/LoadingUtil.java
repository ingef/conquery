package com.bakdata.conquery.integration.common;

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
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.rest.AdminTablesResource;
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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

import static com.bakdata.conquery.ConqueryConstants.EXTENSION_PREPROCESSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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

            ManagedExecution<?> managed = support.getNamespace().getExecutionManager().createQuery(support.getNamespace().getNamespaces(), query, queryId, user, support.getNamespace().getDataset());
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
            List<String> tags = new ArrayList<>();
            // copy csv to tmp folder
            String tableName = rTable.getName();
            for (ResourceFile csv : rTable.getCsv()) {

                FileUtils.copyInputStreamToFile(csv.stream(), new File(support.getTmpDir(), csv.getName()));

                String outFileName = tableName;
                Optional<String> tag = Optional.empty();
                String taglessName = csv.getName();
                final String[] nameSplit = csv.getName().split("\\.");
                if (nameSplit.length == 3) {
                    tag = Optional.of(nameSplit[1]);
                    tags.add(tag.get());
                    outFileName = nameSplit[0];
                    taglessName = nameSplit[0] + "." + nameSplit[2];
                }


                // create import descriptor
                final File descriptionFile = support.getTmpDir().toPath().resolve(csv.getName() + ConqueryConstants.EXTENSION_DESCRIPTION).toFile();
                final File outFile = support.getTmpDir().toPath().resolve(outFileName + (tag.map(s -> "." + s).orElse("")) + EXTENSION_PREPROCESSED).toFile();

                TableImportDescriptor desc = new TableImportDescriptor();
                desc.setName(outFileName);
                desc.setTable(tableName);
                TableInputDescriptor input = new TableInputDescriptor();
                {
                    input.setPrimary(IntegrationUtils.copyOutput(rTable.getPrimaryColumn()));
                    input.setSourceFile(taglessName);
                    input.setOutput(new OutputDescription[rTable.getColumns().length]);
                    for (int i = 0; i < rTable.getColumns().length; i++) {
                        input.getOutput()[i] = IntegrationUtils.copyOutput(rTable.getColumns()[i]);
                    }
                }
                desc.setInputs(new TableInputDescriptor[]{input});

                Jackson.MAPPER.writeValue(descriptionFile, desc);

                descriptions.add(descriptionFile);
                preprocessedFiles.add(outFile);
            }
            // preprocess
            support.preprocessTmp(support.getTmpDir(), descriptions, tags);
        }
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

    public static void updateCqppFile(StandaloneSupport support, File cqpp, ImportId importId, Response.Status.Family expectedResponseFamily, String expectedReason) {
        assertThat(cqpp).exists();

        final URI addImport = HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminTablesResource.class, "updateImport")
                .queryParam("file", cqpp)
                .buildFromMap(Map.of(
                        ResourceConstants.DATASET, support.getDataset().getId(),
                        ResourceConstants.TABLE, importId.getTable(),
                        ResourceConstants.IMPORT_ID, importId
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
