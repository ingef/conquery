package com.bakdata.conquery.models.events;

import com.bakdata.conquery.io.jackson.serializer.CBlockDeserializer;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.*;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.query.queryplan.specific.ConceptNode;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Metadata for connection of {@link Bucket} and {@link Concept}
 * <p>
 * Pre-computed assignment of {@link TreeConcept}.
 */
// TODO move to Bucket
@Getter
@Setter
@JsonDeserialize(using = CBlockDeserializer.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Slf4j
public class CBlock extends IdentifiableImpl<CBlockId> implements NamespacedIdentifiable<CBlockId> {

	/**
	 * Estimate the memory usage of CBlocks.
	 * @param depthEstimate estimate of depth of mostSpecificChildren
	 */
	public static long estimateMemoryBytes(long entities, long entries, double depthEstimate) {
		return Math.round(entities *
						  (
								  Integer.BYTES + Long.BYTES // includedConcepts
								  + Integer.BYTES // minDate
								  + Integer.BYTES // maxDate
						  )
						  + entries * depthEstimate * Integer.BYTES // mostSpecificChildren (rough estimate, not resident on ManagerNode)
		);
	}

	@NsIdRef
	private final Bucket bucket;

	@NotNull
	@NsIdRef
	private final ConceptTreeConnector connector;

	/**
	 * We leverage the fact that a Bucket contains entities from bucketSize * {@link Bucket#getBucket()} to (1 + bucketSize) * {@link Bucket#getBucket()} - 1 to layout our internal structure.
	 * This is maps the first Entities entry in this bucket to 0.
	 */
	private final int root;

	/**
	 * Crude Bloomfilter for Concept inclusion per Entity: Each set bit denotes that the concept (with localId <= 64) or a descendant of that concept (with localId > 64) is present for the entity in this Bucket. 
	 */
	private final long[] includedConceptElementsPerEntity;

	/**
	 * Statistic for fast lookup if entity is of interest.
	 */
	private final CDateRange[] entitySpan;


	/**
	 * Per event: represents the path in a {@link TreeConcept} to optimize lookup.
	 * Nodes in the tree are simply enumerated.
	 */
	private final int[][] mostSpecificChildren;

	public static CBlock createCBlock(ConceptTreeConnector connector, Bucket bucket, int bucketSize) {
		int root = bucket.getBucket() * bucketSize;

		final int[][] mostSpecificChildren = calculateSpecificChildrenPaths(bucket, connector);
		final long[] includedConcepts = calculateConceptElementPathBloomFilter(bucketSize, bucket, mostSpecificChildren);
		final CDateRange[] entitySpans = calculateEntityDateIndices(bucket, bucketSize);

		return new CBlock(bucket, connector, root, includedConcepts, entitySpans, mostSpecificChildren);
	}


	public int[] getEventMostSpecificChild(int event) {
		if (mostSpecificChildren == null) {
			return null;
		}

		return mostSpecificChildren[event];
	}

	public CDateRange getEntityDateRange(int entity) {
		return entitySpan[bucket.getEntityIndex(entity)];
	}

	@Override
	@JsonIgnore
	public CBlockId createId() {
		return new CBlockId(bucket.getId(), connector.getId());
	}

	public boolean isConceptIncluded(int entity, long requiredBits) {
		if (requiredBits == 0L) {
			return true;
		}

		final int index = bucket.getEntityIndex(entity);

		long bits = includedConceptElementsPerEntity[index];

		return (bits & requiredBits) != 0L;
	}

	@Override
	@JsonIgnore
	public Dataset getDataset() {
		return bucket.getDataset();
	}

	/**
	 * Calculates the path for each event from the root of the {@link TreeConcept} to the most specific {@link ConceptTreeChild}
	 * denoted by the individual {@link ConceptTreeChild#getPrefix()}.
	 */
	private static int[][] calculateSpecificChildrenPaths(Bucket bucket, ConceptTreeConnector connector) {

		final Column column = connector.getColumn();

		final TreeConcept treeConcept = connector.getConcept();

		final StringStore stringStore;

		// If we have a column and it is of string-type, we create indices and caches.
		if (column != null && bucket.getStores()[column.getPosition()] instanceof StringStore) {

			stringStore = (StringStore) bucket.getStores()[column.getPosition()];

			// Create index and insert into Tree.
			TreeChildPrefixIndex.putIndexInto(treeConcept);

			treeConcept.initializeIdCache(stringStore, bucket.getImp());
		}
		// No column only possible if we have just one tree element!
		else if(treeConcept.countElements() == 1){
			stringStore = null;
		}
		else {
			throw new IllegalStateException(String.format("Cannot build tree over Connector[%s] without Column", connector.getId()));
		}


		final int[][] mostSpecificChildren = new int[bucket.getNumberOfEvents()][];

		Arrays.fill(mostSpecificChildren, ConceptTreeConnector.NOT_CONTAINED);

		final ConceptTreeCache cache = treeConcept.getCache(bucket.getImp());

		final int[] root = treeConcept.getPrefix();

		for (BucketEntry entry : bucket.entries()) {
			try {
				final int event = entry.getEvent();

				// Events without values are omitted
				// Events can also be filtered, allowing a single table to be used by multiple connectors.
				if (column != null && !bucket.has(event, column)) {
					mostSpecificChildren[event] = Connector.NOT_CONTAINED;
					continue;
				}
				String stringValue = "";
				int valueIndex = -1;

				if (stringStore != null) {
					valueIndex = bucket.getString(event, column);
					stringValue = stringStore.getElement(valueIndex);
				}

				// Lazy evaluation of map to avoid allocations if possible.
				final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(() -> bucket.calculateMap(event));


				if ((connector.getCondition() != null && !connector.getCondition().matches(stringValue, rowMap))) {
					mostSpecificChildren[event] = Connector.NOT_CONTAINED;
					continue;
				}

				ConceptTreeChild child = cache == null
						? treeConcept.findMostSpecificChild(stringValue, rowMap)
						: cache.findMostSpecificChild(valueIndex, stringValue, rowMap);

				// All unresolved elements resolve to the root.
				if (child == null) {
					mostSpecificChildren[event] = root;
					continue;
				}

				// put path into event
				mostSpecificChildren[event] = child.getPrefix();
			}
			catch (ConceptConfigurationException ex) {
				log.error("Failed to resolve event " + bucket + "-" + entry.getEvent() + " against concept " + treeConcept, ex);
			}
		}

		if (cache != null) {
			log.trace(
					"Hits: {}, Misses: {}, Hits/Misses: {}, %Hits: {} (Up to now)",
					cache.getHits(),
					cache.getMisses(),
					(double) cache.getHits() / cache.getMisses(),
					(double) cache.getHits() / (cache.getHits() + cache.getMisses())
			);
		}

		return mostSpecificChildren;
	}

	/**
	 * Calculate for every event a 64 bit long bloom filter, that masks the concept element path within
	 * the first 64 {@link com.bakdata.conquery.models.datasets.concepts.ConceptElement}s of the {@link TreeConcept}.
	 * This is used in the evaluation of a query to quickly decide if an event is of interest by logically ANDing
	 * the bitmask of the event with the bitmask calculated by {@link ConceptNode#calculateBitMask(List)}
	 */
	private static long[] calculateConceptElementPathBloomFilter(int bucketSize, Bucket bucket, int[][] mostSpecificChildren) {
		long[] includedConcepts = new long[bucketSize];
		for (BucketEntry entry : bucket.entries()) {
			final int[] mostSpecificChild = mostSpecificChildren[entry.getEvent()];

			for (int i = 0; i < mostSpecificChild.length; i++) {

				final long mask = calculateBitMask(i, mostSpecificChild);
				includedConcepts[entry.getEntity() - bucketSize*bucket.getBucket()] |= mask;
			}
		}
		return includedConcepts;
	}


	/**
	 * Calculates the bloom filter from the precomputed path to the most specific {@link ConceptTreeChild}.
	 */
	public static long calculateBitMask(int pathIndex, int[] mostSpecificChild) {
		if (pathIndex < 0) {
			return 0;
		}
		if (mostSpecificChild[pathIndex] < 64) {
			return 1L << mostSpecificChild[pathIndex];
		}
		return calculateBitMask(pathIndex-1, mostSpecificChild);
	}



	/**
	 * For every included entity, calculate min and max and store them as statistics in the CBlock.
	 */
	private static CDateRange[] calculateEntityDateIndices(Bucket bucket, int bucketSize) {
		CDateRange[] spans = new CDateRange[bucketSize];
		Arrays.fill(spans, CDateRange.all());

		Table table = bucket.getTable();
		for (Column column : table.getColumns()) {
			if (!column.getType().isDateCompatible()) {
				continue;
			}

			for (BucketEntry entry : bucket.entries()) {
				if (!bucket.has(entry.getEvent(), column)) {
					continue;
				}

				CDateRange range = bucket.getAsDateRange(entry.getEvent(), column);

				final int index = bucket.getEntityIndex(entry.getEntity());

				spans[index] = spans[index].spanClosed(range);
			}
		}

		return spans;
	}
}
