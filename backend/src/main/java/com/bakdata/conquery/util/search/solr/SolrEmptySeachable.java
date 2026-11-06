package com.bakdata.conquery.util.search.solr;

import com.bakdata.conquery.models.datasets.concepts.Searchable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SolrEmptySeachable implements Searchable {

    public static final SolrEmptySeachable INSTANCE = new SolrEmptySeachable();


    @Override
    public String getSearchHandle() {
        return "empty";
    }

    @Override
    public int getMinSuffixLength() {
        return 0;
    }

    @Override
    public boolean isGenerateSuffixes() {
        return false;
    }

    @Override
    public boolean isSearchDisabled() {
        return false;
    }
}
