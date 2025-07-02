package com.bakdata.conquery.util.search.solr;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.search.solr.FilterValueConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.ConceptsProcessor.AutoCompleteResult;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import com.google.common.collect.Iterables;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to abstract/capsule the searchables of the filter away
 */
@AllArgsConstructor
@Slf4j
public class FilterValueSearch {

    private final SelectFilter<?> filter;
    private final SolrProcessor processor;
    private final SolrClient solrClient;
    private final FilterValueConfig filterValueConfig;

    public List<FilterValueIndexer> getSearchesFor(SelectFilter<?> searchable, boolean withEmptySource) {
        List<Searchable> searchReferences = searchable.getSearchReferences();

        if (withEmptySource) {
            // Patchup searchables
            searchReferences.add(SolrEmptySeachable.INSTANCE);
        }

        return searchReferences.stream().map(processor::getIndexerFor).toList();
    }


    /**
     * Creates a filter query (which is cached by solr) for the subset of documents originating from the searchables related to this query.
     *
     * @param withEmptySource Also include the special empty value source in the results, which allows in conquery to filter for empty fields.
     * @return Query string that is a group of the searchable ids for the {@link FilterValueSearch#filter}.
     */
    private @NotNull String buildFilterQuery(boolean withEmptySource) {
        List<FilterValueIndexer> indexers = getSearchesFor(filter, withEmptySource);
        return indexers.stream()
                .map(FilterValueIndexer::getSearchable)
                // The name of the searchable was already escaped at the creation of SolrSearch
                .collect(Collectors.joining(" ", "%s:(".formatted(SolrFrontendValue.Fields.searchable_s), ")"));
    }

    /**
     * <a href="https://lucene.apache.org/core/10_1_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Wildcard_Searches">Query syntax reference</a>
     */
    public AutoCompleteResult topItems(String text, Integer start, @Nullable Integer limit) {

        if (StringUtils.isBlank(text)) {
            // Fallback to wild card if search term is blank search for everything
            text = "_text_:*";

            return sendQuery(text, start, limit, true, true);
        } else {
            text = Arrays.stream(text.split("\\s"))
                    // Skip blanks
                    .filter(Predicate.not(String::isBlank))
                    // Escape
                    .map(ClientUtils::escapeQueryChars)
                    //
                    .map((term) -> {
                        Map<String, String> valuesMap = Map.of("term", term);
                        StringSubstitutor sub = new StringSubstitutor(valuesMap);
                        return sub.replace(filterValueConfig.getQueryTemplate());
                    })
                    .collect(Collectors.joining(" AND "));
            return sendQuery(text, start, limit, false, false);
        }
    }

    private @NotNull AutoCompleteResult sendQuery(String queryString, Integer start, @CheckForNull Integer limit, boolean withEmptySource, boolean sort) {
        SolrQuery query = buildSolrQuery(queryString, start, limit, sort, withEmptySource, true);

        String decodedQuery = URLDecoder.decode(String.valueOf(query), StandardCharsets.UTF_8);
        int queryHash = decodedQuery.hashCode();
        log.info("Query [{}] created: {}", queryHash, decodedQuery);

        try {

            List<FrontendValue> beans = new ArrayList<>();
            final AtomicLong numFound = new AtomicLong();
            QueryResponse response = solrClient.queryAndStreamResponse(query, new StreamingResponseCallback() {
                @Override
                public void streamSolrDocument(SolrDocument doc) {
                    SolrFrontendValue bean = solrClient.getBinder().getBean(SolrFrontendValue.class, doc);
                    beans.add(bean.toFrontendValue());
                }

                @Override
                public void streamDocListInfo(long numFoundCallBack, long start, Float maxScore) {
                    numFound.set(numFoundCallBack);
                }
            });

            log.debug("Query [{}] Found: {} | Collected: {} | QTime: {} | ElapsedTime: {}", queryHash, numFound.get(), beans.size(), response.getQTime(), response.getElapsedTime());

            return new AutoCompleteResult(beans, numFound.get());

        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param withEmptySource Includes the special empty source within the result set. Its single value is usually only needed for the default query.
     * @param collapseSources Collapse documents on {@link SolrFrontendValue#value_s} by source priority. Different sources can hold documents to the same value. Then <code>true</code> the document with the highest priority is included.
     */
    private @NotNull SolrQuery buildSolrQuery(String queryString, Integer start, @CheckForNull Integer limit, boolean sort, boolean withEmptySource, boolean collapseSources) {
        String filterQuery = buildFilterQuery(withEmptySource);

        SolrQuery query = new SolrQuery(queryString);
        query.addFilterQuery(filterQuery);
        query.addField(SolrFrontendValue.Fields.value_s);
        query.addField(SolrFrontendValue.Fields.label_t);
        query.addField(SolrFrontendValue.Fields.optionValue_s);
        query.setStart(start);
        query.setRows(limit);

        if (sort) {
            query.setSort(SolrQuery.SortClause.asc(SolrFrontendValue.Fields.sourcePriority_i));
            query.addSort(SolrQuery.SortClause.asc(filterValueConfig.getDefaultSearchSortField()));
        }

        if (collapseSources) {
            // Collapse the results with equal "value" field. Only the one with the highest score remains.
            // This only works if solr is not sharded (or collapsing documents are on the same shard)
            // We set 'nullPolicy=expand' so we do not suppress the empty label entry
            query.addFilterQuery("{!collapse field=%s min=%s nullPolicy=expand}".formatted(SolrFrontendValue.Fields.value_s, SolrFrontendValue.Fields.sourcePriority_i));
        }

        return query;
    }

    /**
     * Find (almost) exact matches for the provided terms.
     */
    public ConceptsProcessor.ExactFilterValueResult exact(Collection<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return new ConceptsProcessor.ExactFilterValueResult(List.of(), terms);
        }

        final List<String> escapedTerms = terms.stream()
                .filter(Predicate.not(String::isBlank))
                .distinct()
                .map(ClientUtils::escapeQueryChars)
                .toList();

        // Build map of all values (normalized -> original) where we remove the one we found from. This leaves us with the unresolved values in the end
        final Map<String, String> unresolvedMap = terms.stream().collect(Collectors.toMap(String::toLowerCase, Function.identity(), (v1, v2) -> v1));
        final List<FrontendValue> resolved = new ArrayList<>(terms.size());

		/*
		We chunk the values for resolving here so that the request does not bust any URI or query limitations (e.g. "too many boolean operators")
		 */
        int chunkSize = 200;
        int chunkIndex = 1;
        int chunkCount = (escapedTerms.size() + (chunkSize - 1)) / chunkSize;
        final Iterable<List<String>> partition = Iterables.partition(escapedTerms, chunkSize);
        for (List<String> chunk : partition) {

            String finalTerms = chunk.stream().collect(Collectors.joining(" ", "(", ")"));

            if (StringUtils.isBlank(finalTerms)) {
                return new ConceptsProcessor.ExactFilterValueResult(List.of(), terms);
            }

            String collect = Stream.of(
                            SolrFrontendValue.Fields.value_s,
                            SolrFrontendValue.Fields.label_t
                    )
                    .map(field -> "%s:%s".formatted(field, finalTerms))
                    .collect(Collectors.joining(" OR ", "(", ")^=1"));


            // The batchsize is twice the size of the chunk size because a term is often (at most) found in two documents (from the column and from a mapping)
            // Technically a filter could use more than 2 sources, but this is not practical
            final int batchSize = chunkSize * 2;

            final AtomicLong numFound = new AtomicLong();
            try {
                List<FrontendValue> resolvedValues = new ArrayList<>();

                SolrQuery solrQuery = buildSolrQuery(collect, 0, batchSize, false, false, false);

                String decodedQuery = URLDecoder.decode(String.valueOf(solrQuery), StandardCharsets.UTF_8);
                int queryHash = decodedQuery.hashCode();
                log.trace("Query [{}] ({}/{}) created: {}", queryHash, chunkIndex, chunkCount, decodedQuery);

                QueryResponse response = solrClient.queryAndStreamResponse(solrQuery, new StreamingResponseCallback() {
                    @Override
                    public void streamSolrDocument(SolrDocument doc) {
                        if (unresolvedMap.isEmpty()) {
                            // Shortcut: everything was resolved
                            return;
                        }

                        SolrFrontendValue bean = solrClient.getBinder().getBean(SolrFrontendValue.class, doc);


                        // Remove from unresolved and add to resolved values if either value or label matches
                        if (unresolvedMap.remove(bean.value_s.toLowerCase()) != null || (bean.label_t != null && unresolvedMap.remove(bean.label_t.toLowerCase()) != null)) {

                            FrontendValue frontendValue = bean.toFrontendValue();
                            resolvedValues.add(frontendValue);

                        }

                    }

                    @Override
                    public void streamDocListInfo(long numFoundCallBack, long start, Float maxScore) {
                        numFound.set(numFoundCallBack);
                        if (numFoundCallBack > batchSize) {
                            log.warn("Query found more documents ({}) than expected ({}). We expect a term to be found in at most 2 documents (from a column and a mapping).", numFoundCallBack, batchSize);
                        }
                    }
                });
                log.trace("Query [{}] ({}/{}) Found: {} | Collected: {} | QTime: {} | ElapsedTime: {}", queryHash, chunkIndex, chunkCount, numFound.get(), resolvedValues.size(), response.getQTime(), response.getElapsedTime());

                chunkIndex++;
                resolved.addAll(resolvedValues);

            } catch (SolrServerException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new ConceptsProcessor.ExactFilterValueResult(resolved, unresolvedMap.values());
    }

}
