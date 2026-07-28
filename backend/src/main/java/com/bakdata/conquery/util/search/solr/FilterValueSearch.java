package com.bakdata.conquery.util.search.solr;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.search.solr.FilterValueConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.ConceptsProcessor.AutoCompleteResult;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.impl.StreamingBinaryResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
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
 * Helper class to abstract/capsule the {@link Searchable}s of the filter away.
 * The data was/is imported to Solr by a {@link FilterValueIndexer} for each {@link Searchable}, because a single {@link Searchable} might be used by multiple {@link Filter}s.
 */
@AllArgsConstructor
@Slf4j
public class FilterValueSearch {

    public static final int SOLR_MAX_URI_LENGTH = 5000 /* bytes */ ; // with some buffer, actual limit is 8192, but setting this to 6000 already causes failures
	private final SelectFilter<?> filter;
    private final SolrProcessor processor;
    private final SolrClient solrClient;
    private final FilterValueConfig filterValueConfig;

    public List<FilterValueIndexer> getSearchesFor(SelectFilter<?> searchable, boolean withEmptySource) {
        List<Searchable> searchReferences = new ArrayList<>(searchable.getSearchReferences());

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
		}
		text = Arrays.stream(text.split("\\s"))
					 // Skip blanks
					 .filter(Predicate.not(String::isBlank))
					 // Escape
					 .map(ClientUtils::escapeQueryChars)
					 // Resolve Query template
					 .map((term) -> {
						 Map<String, String> valuesMap = Map.of("term", term);
						 StringSubstitutor sub = new StringSubstitutor(valuesMap);
						 return sub.replace(filterValueConfig.getQueryTemplate());
					 })
					 .collect(Collectors.joining(" AND "));
		return sendQuery(text, start, limit, false, false);

	}

    private @NotNull AutoCompleteResult sendQuery(String queryString, Integer start, @CheckForNull Integer limit, boolean withEmptySource, boolean sort) {
        SolrQuery query = buildSolrQuery(queryString, start, limit, sort, withEmptySource, true);

        String decodedQuery = URLDecoder.decode(String.valueOf(query), StandardCharsets.UTF_8);
        int queryHash = decodedQuery.hashCode();
        log.info("Query [{}] created: {}", queryHash, decodedQuery);

        try {

            List<FrontendValue> beans = new ArrayList<>();
            final AtomicLong numFound = new AtomicLong();
            final StreamingResponseCallback callback = new StreamingResponseCallback() {
                @Override
                public void streamSolrDocument(SolrDocument doc) {
                    SolrFrontendValue bean = solrClient.getBinder().getBean(SolrFrontendValue.class, doc);
                    beans.add(bean.toFrontendValue());
                }

                @Override
                public void streamDocListInfo(long numFoundCallBack, long start, Float maxScore) {
                    numFound.set(numFoundCallBack);
                }
            };

            final QueryRequest request = new QueryRequest(query, SolrRequest.METHOD.POST);
            request.setResponseParser(new StreamingBinaryResponseParser(callback));
            final QueryResponse response = request.process(solrClient);

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
     * Find (almost) exact matches for the provided terms. Lowercased search terms are compared to labels and values of solr docs
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


        // TODO also use Solr's POST method to avoid too long URIs
        List<List<String>> chunks = chunkByUriLength(escapedTerms);
        int chunkIndex = 1;
        final int chunkCount = chunks.size();
        int source_count = getSearchesFor(filter, false).size();
        for (List<String> chunk : chunks) {

            String queryString = buildExactQuery(chunk);

            if (StringUtils.isBlank(queryString)) {
                return new ConceptsProcessor.ExactFilterValueResult(List.of(), terms);
            }

            // The batchsize is twice the size of the chunk size because a term is often (at most) found in two documents (from the column and from a mapping)
            final int batchSize = chunk.size() * source_count;

            final AtomicLong numFound = new AtomicLong();
            try {
                List<FrontendValue> resolvedValues = new ArrayList<>();

                // We sort to return value with the highest source priority and get the best description
                SolrQuery solrQuery = buildSolrQuery(queryString, 0, batchSize, true, false, false);


                String decodedQuery = URLDecoder.decode(String.valueOf(solrQuery), StandardCharsets.UTF_8);
                int queryHash = decodedQuery.hashCode();
                log.trace("Query [{}] ({}/{}) created: {}", queryHash, chunkIndex, chunkCount, decodedQuery);

                int queryByteLength = solrQuery.toString().getBytes(StandardCharsets.UTF_8).length;
                log.trace("Query [{}] length in bytes: {}", queryHash, queryByteLength);

                QueryResponse response = solrClient.queryAndStreamResponse(solrQuery, new StreamingResponseCallback() {
                    @Override
                    public void streamSolrDocument(SolrDocument doc) {
                        log.trace("Query [{}] received document: {}", queryHash, doc);
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
                            log.warn("Query found more documents ({}) than expected ({}). We expect a term to be found in at most {} documents (from a column and a mapping).", numFoundCallBack, batchSize, source_count);
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

    private List<List<String>> chunkByUriLength(Collection<String> terms) {
        List<List<String>> result = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String term : terms) {
            if (StringUtils.isBlank(term)) {
                continue;
            }
            current.add(term);

            String query = buildExactQuery(current);

            int size = query.getBytes(StandardCharsets.UTF_8).length;

			if (size <= SOLR_MAX_URI_LENGTH) {
                // Length still fine, let's move on
				continue;
			}

			// remove last term and finish current batch
			current.removeLast();

			if (current.isEmpty()) {
				// single term too large for query
				throw new IllegalArgumentException("Single term is too large for URI. Term: %s".formatted(term));
			} else {
				result.add(current);
				current = new ArrayList<>();
				current.add(term);
			}
		}

        if (!current.isEmpty()) {
            result.add(current);
        }

        return result;
    }

    @NotNull
    private static String buildExactQuery(List<String> chunk) {
        String finalTerms = chunk.stream().collect(Collectors.joining(" ", "(", ")"));

        if (StringUtils.isBlank(finalTerms)) {
            return finalTerms;
        }

        // We are matching on label and value.
        // So if for reason a value is present in multiple sources (map, template, ...) but has different labels
        // both can be found.
		return Stream.of(
						SolrFrontendValue.Fields.value_s,
						SolrFrontendValue.Fields.label_t
				)
				.map(field -> "%s:%s".formatted(field, finalTerms))
				// We are not interested in the result score, so we make it static: ^=1
				.collect(Collectors.joining(" OR ", "(", ")^=1"));
    }

}
