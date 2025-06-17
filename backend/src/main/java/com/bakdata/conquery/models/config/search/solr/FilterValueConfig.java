package com.bakdata.conquery.models.config.search.solr;

import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import javax.annotation.CheckForNull;

@Data
public class FilterValueConfig {

    /**
     * Label for the special empty value to filter for empty entries.
     */
    @NotNull
    String emptyLabel = "No Value";

    /**
     * Effectively the query that is sent to solr after we split the users search phrase into terms on whitespaces and join them together again after template resolving.
     * Joining involves a boolean operator, so parentheses might be needed.
     * The format string only gets a single argument, so refer to the argument using <code>%1$s</code> if you want to use it multiple times.
     */
    @NotEmpty
    String queryTemplate = "%1$s value_s:\"%1$s\"^100";

    /**
     * Determines the field by which the results of the default query (no user input, just results) are sorted.
     * This is only possible for fields that are sortable (docValues or indexed).
     */
    @CheckForNull
    String defaultSearchSortField = SolrFrontendValue.Fields.value_s;

    /**
     * By default, for each value in a column a solr document is created. The id of this solr-document uses the column id and the column value.
     * This can cause a large number of documents, many referring to the same <code>value</code>.
     * When this flag is <code>true</code>, not the column id but its name is used to form the document id, hence abstracting over all columns of the same name.
     * <p/>
     * Beware that you need to ensure that equally named columns used in filters are also based on the same set of values. Otherwise, you might encounter unexpected
     * query results.
     */
    boolean combineEquallyNamedColumns = false;
}