package com.bakdata.conquery.integration.sql;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SqlTestSpecParser {

    private final SqlStandaloneSupport support;

    public SqlTestSpecParser() {
        this.support = new SqlStandaloneSupport(new Dataset("test"));
    }

    /**
     * Imports the required tables and concepts for mapping tables and concepts.
     * MUST BE CALLED before trying to use parseQuery().
     */
    public void importRequiredData(SqlIntegrationTest testSpec) throws JSONException, IOException {
        importTables(testSpec);
        importConcepts(testSpec);
    }

    /**
     * Tables and concepts MUST be imported upfront calling importRequiredData().
     * Converts a raw JSON query into the corresponding CQElement representatives.
     */
    public Query parseQuery(JsonNode rawQuery) throws JSONException, IOException {
        JavaType queryType = Jackson.MAPPER.getTypeFactory().constructParametricType(Query.class, new JavaType[0]);
        return parseSubTree(this.support, rawQuery, queryType);
    }

    public static <T> T parseSubTree(SqlStandaloneSupport support, JsonNode node, JavaType expectedType) throws IOException {
        final ObjectMapper om = Jackson.MAPPER.copy();
        ObjectMapper mapper = support.getDataset().injectIntoNew(
                new SingletonNamespaceCollection(support.getStorage().getCentralRegistry())
                        .injectIntoNew(
                                om.addHandler(new DatasetPlaceHolderFiller(support))
                        )
        );
        return mapper.readerFor(expectedType).readValue(node);
    }

    public static <T> List<T> parseSubTreeList(SqlStandaloneSupport support, ArrayNode node, Class<?> expectedType, Consumer<T> modifierBeforeValidation) throws IOException {
        final ObjectMapper om = Jackson.MAPPER.copy();
        ObjectMapper mapper = support.getDataset().injectInto(
                new SingletonNamespaceCollection(support.getStorage().getCentralRegistry()).injectIntoNew(
                        om.addHandler(new DatasetPlaceHolderFiller(support))
                )
        );

        mapper.setConfig(mapper.getDeserializationConfig().withView(View.Api.class));

        List<T> result = new ArrayList<>(node.size());
        for (var child : node) {
            T value;
            try {
                value = mapper.readerFor(expectedType).readValue(child);
            } catch (Exception e) {
                if (child.isValueNode()) {
                    String potentialPath = child.textValue();
                    try {
                        value = mapper.readerFor(expectedType).readValue(IntegrationTest.class.getResource(potentialPath));
                    } catch (Exception e2) {
                        throw new RuntimeException("Could not parse value " + potentialPath, e2);
                    }
                } else {
                    throw e;
                }
            }

            if (modifierBeforeValidation != null) {
                modifierBeforeValidation.accept(value);
            }
            result.add(value);
        }
        return result;
    }

    private void importTables(SqlIntegrationTest testSpec) {
        for (RequiredTable rTable : testSpec.getContent().getTables()) {
            final Table table = rTable.toTable(
                    this.support.getDataset(),
                    this.support.getStorage().getCentralRegistry()
            );
            this.support.getStorage().addTable(table);
        }
    }

    private void importConcepts(SqlIntegrationTest testSpec) throws IOException {
        List<Concept<?>> concepts = parseSubTreeList(
                this.support,
                testSpec.getRawConcepts(),
                Concept.class,
                concept -> concept.setDataset(this.support.getDataset())
        );

        for (Concept<?> concept : concepts)
            this.support.getStorage().updateConcept(concept);
    }

    /**
     * Replaces occurrences of the string "${dataset}" with the id of the current dataset of the {@link SqlStandaloneSupport}.
     */
    @RequiredArgsConstructor
    private static class DatasetPlaceHolderFiller extends DeserializationProblemHandler {

        private final SqlStandaloneSupport support;

        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg) throws IOException {
            IdUtil.Parser parser = IdUtil.<Id<Identifiable<?>>>createParser((Class) targetType);
            return parser.parsePrefixed(support.getDataset().getId().toString(), valueToConvert);
        }
    }

}
