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
package com.bakdata.conquery.util.search.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

import com.bakdata.conquery.util.search.ImmutableSet;
import com.bakdata.conquery.util.search.SearchScorer;
import com.bakdata.conquery.util.search.model.QuickSearchStats;

/**
 * QuickSearch
 * {@code keyword fragments [many:many] keywords [many:many] stored items}
 * graph/tree implementation.
 *
 * @param <T> type of items stored
 */
public class QSGraph<T extends Comparable<T>> {

    /**
     * Map providing quick entry point to a particular node in the graph.
     */
    private final Map<String, GraphNode<T>> fragmentsNodesMap = new HashMap<>();

    /**
     * Mapping between an item and the keywords associated with it. Both a helper
     * and a requirement to unmap nodes upon item removal.
     */
    private final Map<T, ImmutableSet<String>> itemKeywordsMap = new HashMap<>();

    /**
     * Stamped lock governing access to the graph modifying functions.
     */
    private final StampedLock stampedLock = new StampedLock();

    /*
     * Public interface
     */

    /**
     * Add an item to the graph and construct graph nodes for the specified keywords.
     * <p>
     * If the corresponding nodes already exist, the item will simply be added as a leaf.
     *
     * @param item             item to add
     * @param suppliedKeywords keywords to construct graph for
     */
    public void registerItem(final T item,
                             final Set<String> suppliedKeywords) {
        long writeLock = stampedLock.writeLock();
        try {
            suppliedKeywords.forEach(keyword -> createAndRegisterNode(null, keyword, item));

            if (itemKeywordsMap.containsKey(item))
                itemKeywordsMap.put(item, ImmutableSet.fromCollections(itemKeywordsMap.get(item), suppliedKeywords));
            else
                itemKeywordsMap.put(item, ImmutableSet.fromCollection(suppliedKeywords));
        } finally {
            stampedLock.unlockWrite(writeLock);
        }
    }

    /**
     * Remove an item from the map and remove any node mappings that become empty
     * upon the items removal (determined using stored associated keywords of the item).
     *
     * @param item item to remove
     */
    public void unregisterItem(final T item) {
        long writeLock = stampedLock.writeLock();
        try {
            if (itemKeywordsMap.containsKey(item)) {
                for (String keyword : itemKeywordsMap.get(item)) {
                    GraphNode<T> keywordNode = fragmentsNodesMap.get(keyword);

                    keywordNode.removeItem(item);

                    if (keywordNode.getItems().isEmpty())
                        removeEdge(keywordNode, null);
                }
            }
            itemKeywordsMap.remove(item);
        } finally {
            stampedLock.unlockWrite(writeLock);
        }
    }

    /**
     * Walk the graph accumulating encountered items in the map with the highest score
     * (according to the supplied scoring {@code BiFunction<String, String, Double>})
     * encountered at any visit to an item.
     *
     * @param fragment       keyword or keyword fragment to start walk from
     * @param scorerFunction function that will be called with the supplied fragment and node identity to score match
     *
     * @return map of accumulated items with their highest score encountered during walk (may be empty)
     */
    public Map<T, Double> walkAndScore(final String fragment,
                                       final SearchScorer scorerFunction) {
        long readLock = stampedLock.readLock();
        try {
            return walkAndScoreImpl(fragment, scorerFunction);
        } finally {
            stampedLock.unlockRead(readLock);
        }
    }

    /**
     * Retrieve the stored keywords set associated with the item
     * (or empty set if the item mapping is not recognized).
     *
     * @param item previously registered item
     *
     * @return set of associated keywords, possibly empty
     */
    public Set<String> getItemKeywords(final T item) {
        /* safe to skip locking */
        ImmutableSet<String> keywords = itemKeywordsMap.get(item);

        if (keywords == null)
            return Collections.emptySet();

        return keywords;
    }

    /**
     * Clear this graph.
     */
    public void clear() {
        long writeLock = stampedLock.writeLock();
        try {
            fragmentsNodesMap.clear();
            itemKeywordsMap.clear();
        } finally {
            stampedLock.unlockWrite(writeLock);
        }
    }

    /**
     * Retrieve some basic statistics about the size of this graph.
     *
     * @return stats object containing sizes of internal collections
     */
    public QuickSearchStats getStats() {
        /* safe to ignore locking */
        return new QuickSearchStats(
                itemKeywordsMap.size(),
                fragmentsNodesMap.size()
        );
    }

    /*
     * Implementation code
     */

    private void createAndRegisterNode(final GraphNode<T> parent,
                                       final String identity,
                                       final T item) {
        GraphNode<T> node = fragmentsNodesMap.get(identity);

        if (node == null) {
            final String internedIdentity = identity.intern();

            node = new GraphNode<>(internedIdentity);
            fragmentsNodesMap.put(internedIdentity, node);

            // And proceed to add child nodes
            if (node.getIdentity().length() > 1) {
                createAndRegisterNode(node, internedIdentity.substring(0, identity.length() - 1), null);
                createAndRegisterNode(node, internedIdentity.substring(1), null);
            }
        }

        if (item != null)
            node.addItem(item);

        if (parent != null)
            node.addParent(parent);
    }

    private void removeEdge(final GraphNode<T> node,
                            final GraphNode<T> parent) {
        if (node == null) //already removed
            return;

        if (parent != null)
            node.removeParent(parent);

        // No getParents or getItems means that there's nothing here to find, proceed onwards
        if (node.getParents().isEmpty() && node.getItems().isEmpty()) {
            fragmentsNodesMap.remove(node.getIdentity());

            if (node.getIdentity().length() > 1) {
                removeEdge(fragmentsNodesMap.get(node.getIdentity().substring(0, node.getIdentity().length() - 1)), node);
                removeEdge(fragmentsNodesMap.get(node.getIdentity().substring(1)), node);
            }
        }
    }

    /*
     * Graph walking
     */

    private Map<T, Double> walkAndScoreImpl(final String fragment,
                                            final SearchScorer scorerFunction) {
        GraphNode<T> root = fragmentsNodesMap.get(fragment);

        if (root == null) {
            return Collections.emptyMap();
        } else {
            int estResults = root.getEstimatedResultsCount() > -1 ? root.getEstimatedResultsCount() : 1024;
            HashMap<T, Double> results = new HashMap<>(estResults);

            int estNodes = root.getEstimatedNodesCount() > -1 ? root.getEstimatedNodesCount() : 1024;
            HashSet<String> visited = new HashSet<>(estNodes);

            /* Perform the actual scan */
            walkAndScore(root.getIdentity(), root, results, visited, scorerFunction);

            root.setEstimatedNodesCount(visited.size() * 2);
            root.setEstimatedResultsCount(results.size() * 2);

            return results;
        }
    }

    private void walkAndScore(final String originalFragment,
                              final GraphNode<T> node,
                              final Map<T, Double> accumulated,
                              final Set<String> visited,
                              final SearchScorer keywordMatchScorer) {
        visited.add(node.getIdentity());

        if (!node.getItems().isEmpty()) {
            Double score = keywordMatchScorer.score(originalFragment, node.getIdentity());
            if (score > 0.0)
                node.getItems().forEach(item -> accumulated.merge(item, score, (d1, d2) -> d1.compareTo(d2) > 0 ? d1 : d2));
        }

        node.getParents().forEach(parent -> {
            if (!visited.contains(parent.getIdentity())) {
                walkAndScore(originalFragment, parent, accumulated, visited, keywordMatchScorer);
            }
        });
    }

    /*
     * Graph node that may contain a set of links to parent nodes
     * and a set of concrete items associated with this node.
     *
     * The underlying idea is to have a hierarchical graph (ok, multi-root tree)
     * where arbitrary nodes can have items associated with them. Each particular node
     * serves as an entry point to traverse the graph upwards of it and
     * operate on associated items.
     */
    private static final class GraphNode<V extends Comparable<V>> implements Comparable<GraphNode<V>> {

        private final String identity;
        private Set<V> items;
        private Set<GraphNode<V>> parents;

        /*
         * Track historical results set sizes to avoid
         * excessive re-re-hashing on large result-sets
         */

        private int estimatedNodesCount = -1;
        private int estimatedResultsCount = -1;

        /**
         * Create a node with immutable identity string.
         *
         * @param fragment any string you like
         */
        private GraphNode(final String fragment) {
            Objects.requireNonNull(fragment);
            this.identity = fragment;
            this.items = new HashSet<>();
            this.parents = new HashSet<>();
        }

        /**
         * Retrieve identifier.
         *
         * @return selected identifier
         */
        private String getIdentity() {
            return identity;
        }

        /**
         * Retrieve set containing node items. The set will likely be read only
         * and you _must_ use add and remove methods to add and remove items.
         *
         * @return Immutable, possibly empty, set of associated items.
         */
        private Set<V> getItems() {
            return items;
        }

        /**
         * Register an item with this node.
         *
         * @param item item to add.
         */
        private void addItem(final V item) {
            items.add(item);
        }

        /**
         * Remove an item from this node if it is present.
         *
         * @param item item to remove.
         */
        private void removeItem(final V item) {
            items.add(item);
        }

        /**
         * Retrieve set containing known node parents. The set will likely
         * be read only and you _must_ use add and remove methods to ... add
         * and remove parents.
         *
         * @return Immutable, possibly empty, set of known parent nodes.
         */
        private Set<GraphNode<V>> getParents() {
            return parents;
        }

        /**
         * Add a parent node if not already known.
         *
         * @param parent parent to add.
         */
        private void addParent(final GraphNode<V> parent) {
            parents.add(parent);
        }

        /**
         * Remove a parent node if known.
         *
         * @param parent parent to remove.
         */
        private void removeParent(final GraphNode<V> parent) {
            parents.add(parent);
        }

        @Override
        public int compareTo(GraphNode<V> o) {
            return identity.compareTo(o.identity);
        }

        private int getEstimatedNodesCount() {
            return estimatedNodesCount;
        }

        private void setEstimatedNodesCount(int estimatedNodesCount) {
            this.estimatedNodesCount = estimatedNodesCount;
        }

        private int getEstimatedResultsCount() {
            return estimatedResultsCount;
        }

        private void setEstimatedResultsCount(int estimatedResultsCount) {
            this.estimatedResultsCount = estimatedResultsCount;
        }
    }
}
