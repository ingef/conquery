package com.bakdata.conquery.util.search.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import com.bakdata.conquery.models.messages.namespaces.specific.RegisterColumnValues;
import com.bakdata.conquery.util.search.solr.entities.SolrFrontendValue;
import com.google.common.base.Stopwatch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * Gathers solr documents into chunks and submits them asynchronously to the solr client.
 */
@Slf4j
@RequiredArgsConstructor
public class ChunkSubmitter {

	@Getter
	private final String searchable;

	@Getter
	private final SolrClient solrClient;

	private final int updateChunkSize;

	/**
	 * Single threaded runtime for the chunk submitter.
	 */
	private final ExecutorService executor;

	private Throwable clientError = null;

	/**
	 * Current chunk that is filled with column values through {@link RegisterColumnValues} -- only a single thread/job accesses it at a time.
	 * A filled chunk is inserted into {@link ChunkSubmitter#openChunksQueue} and is concurrently extracted by {@link ChunkSubmitter#submitter}
	 */
	private Collection<SolrFrontendValue> openChunk = new ArrayList<>();


	/**
	 * Buffer docs to send repartitioned update chunks.
	 */
	private final Queue<Collection<SolrFrontendValue>> openChunksQueue = new ConcurrentLinkedQueue<>();

	/**
	 * Asynchronous submitter for solr docs.
	 * @implNote We start here with a noop that we can chain on.
	 */
	private CompletableFuture<Void> submitter = CompletableFuture.completedFuture(null);

	public void insertIntoChunk(SolrFrontendValue solrFrontendValue) {

		openChunk.add(solrFrontendValue);

		if (openChunk.size() >= updateChunkSize) {
			openChunksQueue.add(openChunk);
			openChunk = new ArrayList<>(updateChunkSize);

			// Check if there is already a submitter working on pending docs
			if (submitter.isDone()) {
				// Start submitting so we can start free pending documents
				submitChunk();
			}
		}
	}

	private synchronized CompletableFuture<Void> submitChunk() {
		submitter =  submitter.thenRunAsync(
				() -> {
					if (openChunksQueue.isEmpty()) {
						return;
					}

					// We chunk here for performance.
					// Too many small document request cause a lot of overhead.
					// A too large chunk slows request submission and solr.
					do  {
						int chunksCount = openChunksQueue.size();

						Collection<SolrFrontendValue> chunk = openChunksQueue.poll();
						log.debug("Adding {} (of currently ca. {}) documents for {}", chunk.size(), chunksCount*updateChunkSize, searchable);
						registerValues(chunk);
					} while (!openChunksQueue.isEmpty());
				},
				executor
		);

		return submitter;
	}


	private void registerValues(Collection<SolrFrontendValue> solrFrontendValues) {
		if (clientError != null) {
			log.error("Cannot index value for {}, because client had an error previously", searchable);
			return;
		}

		if (solrFrontendValues.isEmpty()) {
			// Avoid "BaseHttpSolrClient$RemoteSolrException: ... missing content stream" on empty collection
			return;
		}

		try {
			Stopwatch stopwatch = Stopwatch.createStarted();
			log.trace("BEGIN registering {} values to {} for {}", solrFrontendValues.size(), solrClient.getDefaultCollection(), searchable);
			solrClient.addBeans(solrFrontendValues); // do not commit yet
			log.trace("DONE registering {} values to {} for {} in {}", solrFrontendValues.size(), solrClient.getDefaultCollection(), searchable, stopwatch);

		}
		catch (SolrServerException | IOException e) {
			clientError = e;
			try {
				solrClient.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			throw new IllegalStateException("Unable to register values for searchable '%s'".formatted(searchable), e);
		}
	}

	public void finalizeSubmit() {
		if (openChunk.isEmpty() && openChunksQueue.isEmpty()) {
			return;
		}

		openChunksQueue.add(openChunk);

		// Commit what is left and reset
		log.trace("Commiting the last {} documents of {}", openChunk.size(), searchable);
		openChunk = new ArrayList<>();
		submitChunk().join();
		openChunksQueue.clear();
	}
}
