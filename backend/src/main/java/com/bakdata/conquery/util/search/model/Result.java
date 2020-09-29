/*
 *                                     //
 * Copyright 2016 Karlis Zigurs (http://zigurs.com)
 *                                   //
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bakdata.conquery.util.search.model;

import java.util.List;

/**
 * Container for augmented com.bakdata.conquery.util.search results.
 *
 * @param <T> wrapped response item type
 */
public class Result<T> {

    private final String searchString;
    private final List<ResultItem<T>> responseResultItems;
    private final int requestedMaxItems;

    /**
     * Construct an instance
     *
     * @param searchString        com.bakdata.conquery.util.search string that generated this result set
     * @param responseResultItems found items
     * @param requestedMaxItems   requested results count
     */
    public Result(final String searchString,
                  final List<ResultItem<T>> responseResultItems,
                  final int requestedMaxItems) {
        this.searchString = searchString;
        this.responseResultItems = responseResultItems;
        this.requestedMaxItems = requestedMaxItems;
    }

    /**
     * Query.
     *
     * @return original com.bakdata.conquery.util.search string
     */
    public String getSearchString() {
        return searchString;
    }

    /**
     * Query.
     *
     * @return list of zero to n top scoring com.bakdata.conquery.util.search items
     */
    public List<ResultItem<T>> getResponseResultItems() {
        return responseResultItems;
    }

    /**
     * Query.
     *
     * @return number of top items requested in the com.bakdata.conquery.util.search request
     */
    public int getRequestedMaxItems() {
        return requestedMaxItems;
    }
}
