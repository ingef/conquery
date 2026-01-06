package com.bakdata.conquery.models.config.search.solr;

import java.util.Collections;
import java.util.Map;
import javax.annotation.CheckForNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * Solr search and index configurations that are specific to {@link com.bakdata.conquery.apiv1.query.concept.filter.FilterValue}s.
 */
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
     * The format string only gets a single argument, so refer to the argument using <code>${term}</code>. The template is interpreted by {@link org.apache.commons.text.StringSubstitutor}.
     * <p/>
     * An incoming search string is split on white spaces into terms. These terms are each (<code>${term}</code>) escaped and applied to this template and then concatenated with an <code> AND </code>.
     * If you have a complex expression, consider to surround your template with parentheses, so the concatenating "AND"s apply to the whole template.
     */
    @NotEmpty
    private String queryTemplate = "( ${term} value_s:\"${term}\"^100 )";

    /**
     * Determines the field by which the results of the default query (no user input, just results) are sorted.
     * This is only possible for fields that are sortable (docValues or indexed).
     */
    @CheckForNull
    private String defaultSearchSortField = SolrFrontendValue.Fields.value_s;

    private boolean sharedDocumentsOnSecondaryId = false;

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
    private Map<@NotBlank String,Map<@NotBlank String,@NotBlank String>> sharedColumnDocuments = Collections.emptyMap();



	/**
	 * Number of documents in an update request to solr.
	 * @implNote It seems that the number of documents per update request is a more limiting rather than the size of each document. So while sending batches of 100
	 * "larger" documents might go through, sending batches of 200 smaller documents may lead to connection losses / timeouts.
	 */
	@Min(1)
	private int updateChunkSize = 100;

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