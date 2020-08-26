package com.bakdata.conquery.util.search;
/*
 * Originally adapted from https://github.com/karliszigurs/QuickSearch, but using mutable Sets/Maps to avoid costly reallocations when adding a new elements, as this was slowing down our startup by quite the big margin.
 * Additionally, some new Methods have been added, for example unlimited searches, which we use for our resolves.
 *
 * All original Credit goes to @Karliszigurs
*/