package com.bakdata.conquery.io.storage;

import static com.bakdata.conquery.models.execution.Shareable.log;

import java.io.Closeable;
import java.io.IOException;

import com.bakdata.conquery.io.storage.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MoreCollectors;
import com.google.common.graph.Graph;
import com.google.common.graph.Traverser;

public abstract class ConqueryStorage implements Closeable {

	public abstract CentralRegistry getCentralRegistry();

	public abstract void openStores(ObjectMapper objectMapper);

	protected abstract Graph<KeyIncludingStore<?, ?>> getStoreDependencies();

	public void loadData() {

		final Graph<KeyIncludingStore<?, ?>> loadGraph = getStoreDependencies();

		final KeyIncludingStore<?, ?> root = findRootNode(loadGraph);


		//TODO this can also be used to load data in parallel
		for (KeyIncludingStore<?, ?> store : Traverser.forGraph(loadGraph).breadthFirst(root)) {
			store.loadData();
		}

		log.info("Done reading {}", this);
	}

	private static KeyIncludingStore<?, ?> findRootNode(Graph<KeyIncludingStore<?, ?>> loadGraph) {
		return loadGraph.nodes().stream()
						.filter(node -> loadGraph.inDegree(node) == 0)
						.collect(MoreCollectors.onlyElement());
	}

	/**
	 * Delete the storage's contents.
	 */
	public void clear() {
		getCentralRegistry().clear();

		final Graph<KeyIncludingStore<?, ?>> loadGraph = getStoreDependencies();

		final KeyIncludingStore<?, ?> root = findRootNode(loadGraph);


		for (KeyIncludingStore<?, ?> store : Traverser.forGraph(getStoreDependencies()).depthFirstPostOrder(root)) {
			store.clear();
		}
	}

	/**
	 * Remove the storage.
	 */
	public void removeStorage(){

		final Graph<KeyIncludingStore<?, ?>> loadGraph = getStoreDependencies();

		final KeyIncludingStore<?, ?> root = findRootNode(loadGraph);

		for (KeyIncludingStore<?, ?> store : Traverser.forGraph(getStoreDependencies()).depthFirstPostOrder(root)) {
			store.clear();
		}
	}

	public void close() throws IOException {
		final Graph<KeyIncludingStore<?, ?>> loadGraph = getStoreDependencies();

		final KeyIncludingStore<?, ?> root = findRootNode(loadGraph);

		for (KeyIncludingStore<?, ?> store : Traverser.forGraph(getStoreDependencies()).depthFirstPostOrder(root)) {
			store.close();
		}
	}
}
