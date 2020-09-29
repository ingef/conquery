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

/**
 * Container for quick com.bakdata.conquery.util.search instance stats
 */
public class QuickSearchStats {

    private final int items;
    private final int fragments;

    /**
     * Construct an instance.
     *
     * @param items     in the com.bakdata.conquery.util.search instance
     * @param fragments in the com.bakdata.conquery.util.search instance
     */
    public QuickSearchStats(final int items,
                            final int fragments) {
        this.items = items;
        this.fragments = fragments;
    }

    /**
     * Query.
     *
     * @return number of items
     */
    public int getItems() {
        return items;
    }

    /**
     * Query.
     *
     * @return fragments in the instance
     */
    public int getFragments() {
        return fragments;
    }
}
