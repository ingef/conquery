package com.bakdata.conquery.models.config.search.solr;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
public class FilterValueConfig {

    /**
     * Label for the special empty value to filter for empty entries.
     */
    @NotNull
    private String emptyLabel = "No Value";

    /**
     * Effectively the query that is sent to solr after we split the users search phrase into terms on whitespaces and join them together again after template resolving.
     * Joining involves a boolean operator, so parentheses might be needed.
     * The format string only gets a single argument, so refer to the argument using <code>%1$s</code> if you want to use it multiple times.
     */
    @NotEmpty
    private String queryTemplate = "%1$s value_s:\"%1$s\"^100";

    /**
     * Determines the field by which the results of the default query (no user input, just results) are sorted.
     * This is only possible for fields that are sortable (docValues or indexed).
     */
    @CheckForNull
    private String defaultSearchSortField = SolrFrontendValue.Fields.value_s;

    private boolean sharedDocumentsOnSecondaryId = true;

    /**
     * By default, for each value in a column a solr document is created. The id of this solr-document uses the column id and the column value.
     * This can cause a large number of documents, many referring to the same <code>value</code>.
     * When this map is populated, not the column id but a group name is used to form the document id, hence abstracting over all columns that point to the same group.
     * <p/>
     * Beware that you need to ensure that equally named columns used in filters are also based on the same set of values. Otherwise, you might encounter unexpected
     * query results.

     * TableName -> ColumnName -> GroupName
     * <code>
     *     {
     *         "zoo_table": {
     *             "animal": "animals",
     *             "sponsor": "proteges"
     *         },
     *         "pet_table": {
     *             "pet": "animals",
     *             "owner": "proteges"
     *         }
     *     }
     * </code>
     */
    private Map<@NotBlank String,Map<@NotBlank String,@NotBlank String>> sharedColumnDocuments = new HashMap<>();

    /**
     * Returns the shared group for the column, if documents should be shared.
     *
     * @param column The column id that might belong to a group
     * @return The group name or <code>null</code>.
     */
    @JsonIgnore
    public String getColumnGroup(Column column) {
        String tableName = column.getTable().getName();
        String columnName = column.getName();
        String groupName = sharedColumnDocuments.getOrDefault(tableName, Collections.emptyMap()).get(columnName);
        if (groupName != null) {
            return groupName;
        }

        if (sharedDocumentsOnSecondaryId && column.getSecondaryId() != null) {
            groupName = column.getSecondaryId().getName();

        }
        return groupName;
    }
}