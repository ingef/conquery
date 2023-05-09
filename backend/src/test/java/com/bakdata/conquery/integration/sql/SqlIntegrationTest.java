package com.bakdata.conquery.integration.sql;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@Setter
public class SqlIntegrationTest {

    private final String EXPECTED_SQL_FILENAME = "expected.sql";

    @NotNull
    private String label;

    @NotNull
    @JsonProperty("query")
    private JsonNode rawQuery;

    @Valid
    @NotNull
    private RequiredData content;

    @NotNull
    @JsonProperty("concepts")
    private ArrayNode rawConcepts;

    @JsonIgnore
    private Query query;

    @JsonIgnore
    private Path specDir;

    @SneakyThrows
    public static SqlIntegrationTest fromJsonSpec(Path path) {
        SqlIntegrationTest test = readSpecFromJson(path);
        test.setQuery(parseSubTrees(test));
		test.setSpecDir(path.getParent());
        return test;
    }

    public String getExpectedSql() throws IOException {
        Path expectedSqlFile = this.specDir.resolve(EXPECTED_SQL_FILENAME);
        return Files.readString(expectedSqlFile).trim();
    }

    private static SqlIntegrationTest readSpecFromJson(Path path) throws IOException {
        final ObjectReader objectReader = Jackson.MAPPER.readerFor(SqlIntegrationTest.class);
        return objectReader.readValue(Files.readString(path));
    }

    private static Query parseSubTrees(SqlIntegrationTest test) throws JSONException, IOException {
        final SqlTestSpecParser specParser = new SqlTestSpecParser();
        specParser.importRequiredData(test);
        return specParser.parseQuery(test.getRawQuery());
    }

}
