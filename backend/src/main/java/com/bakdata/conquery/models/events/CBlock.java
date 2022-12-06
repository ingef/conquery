package com.bakdata.conquery.models.events;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.CBlockDeserializer;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefKeys;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeCache;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeChildPrefixIndex;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
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
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

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
@ToString(onlyExplicitlyIncluded = true)
public class CBlock extends IdentifiableImpl<CBlockId> implements NamespacedIdentifiable<CBlockId> {

	/**
	 * Estimate the memory usage of CBlocks.
	 *
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

	@ToString.Include
	@NsIdRef
	private final Bucket bucket;

	@NotNull
	@NsIdRef
	@ToString.Include
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

	@NsIdRefKeys //TODO deserializer for Int2ObjectMap and IntSet
	private final Map<Column, Int2ObjectMap<?>> columnIndices;


	public static CBlock createCBlock(ConceptTreeConnector connector, Bucket bucket, int bucketSize) {
		final int root = bucket.getBucket() * bucketSize;

		final int[][] mostSpecificChildren = calculateSpecificChildrenPaths(bucket, connector);
		final long[] includedConcepts = calculateConceptElementPathBloomFilter(bucketSize, bucket, mostSpecificChildren);
		final CDateRange[] entitySpans = calculateEntityDateIndices(bucket, bucketSize);

		Map<Column, Int2ObjectMap<?>> indices = new HashMap<>();

		for (Column column : bucket.getTable().getColumns()) {
			Int2ObjectMap<?> columnIndex = createColumnIndex(column, bucket);

			indices.put(column, columnIndex);
		}

		return new CBlock(bucket, connector, root, includedConcepts, entitySpans, mostSpecificChildren, indices);
	}


	public int[] getPathToMostSpecificChild(int event) {
		if (mostSpecificChildren == null) {
			return null;
		}

		return mostSpecificChildren[event];
	}

	public int getMostSpecificChildLocalId(int event) {
		if (mostSpecificChildren == null) {
			return -1;
		}

		final int[] mostSpecificChild = mostSpecificChildren[event];
		return mostSpecificChild[mostSpecificChild.length - 1];
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

		final long bits = includedConceptElementsPerEntity[index];

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
		else if (treeConcept.countElements() == 1) {
			stringStore = null;
		}
		else {
			throw new IllegalStateException(String.format("Cannot build tree over Connector[%s] without Column", connector.getId()));
		}


		final int[][] mostSpecificChildren = new int[bucket.getNumberOfEvents()][];

		Arrays.fill(mostSpecificChildren, ConceptTreeConnector.NOT_CONTAINED);

		final ConceptTreeCache cache = treeConcept.getCache(bucket.getImp());

		final int[] root = treeConcept.getPrefix();

		for (int event = 0; event < bucket.getNumberOfEvents(); event++) {


			try {
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
				// Copy event for closure.
				final int _event = event;
				final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(() -> bucket.calculateMap(_event));


				if ((connector.getCondition() != null && !connector.getCondition().matches(stringValue, rowMap))) {
					mostSpecificChildren[event] = Connector.NOT_CONTAINED;
					continue;
				}

				final ConceptTreeChild child = cache == null
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
				log.error("Failed to resolve event {}-{} against concept {}", bucket, event, treeConcept, ex);
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
		final long[] includedConcepts = new long[bucketSize];

		for (int entity : bucket.getEntities()) {

			final int entityIndex = bucket.getEntityIndex(entity);
			final int end = bucket.getEntityEnd(entity);

			for (int event = bucket.getEntityStart(entity); event < end; event++) {

				final int[] mostSpecificChild = mostSpecificChildren[event];

				for (int i = 0; i < mostSpecificChild.length; i++) {

					final long mask = calculateBitMask(i, mostSpecificChild);

					includedConcepts[entityIndex] |= mask;
				}
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
		if (mostSpecificChild[pathIndex] < Long.SIZE) {
			return 1L << mostSpecificChild[pathIndex];
		}
		return calculateBitMask(pathIndex - 1, mostSpecificChild);
	}


	/**
	 * For every included entity, calculate min and max and store them as statistics in the CBlock.
	 *
	 * @implNote This is an unrolled implementation of {@link CDateRange#span(CDateRange)}.
	 */
	private static CDateRange[] calculateEntityDateIndices(Bucket bucket, int bucketSize) {
		final CDateRange[] spans = new CDateRange[bucketSize];

		final Table table = bucket.getTable();


		for (Column column : table.getColumns()) {
			if (!column.getType().isDateCompatible()) {
				continue;
			}

			for (int entity : bucket.getEntities()) {
				final int index = bucket.getEntityIndex(entity);
				final int end = bucket.getEntityEnd(entity);

				// We unroll span for the whole bucket/entity, this avoids costly reallocation in a loop
				// First we initialize the values to illegal values, making Min/Max easier
				int max = Integer.MIN_VALUE;
				int min = Integer.MAX_VALUE;


				for (int event = bucket.getEntityStart(entity); event < end; event++) {
					if (!bucket.has(event, column)) {
						continue;
					}

					final CDateRange range = bucket.getAsDateRange(event, column);

					{
						final int minValue = range.getMinValue();

						max = Math.max(max, minValue);
						min = Math.min(min, minValue);
					}

					{
						final int maxValue = range.getMaxValue();

						max = Math.max(max, maxValue);
						min = Math.min(min, maxValue);
					}
				}

				final CDateRange span = calculateSpan(max, min, spans[index]);
				spans[index] = span;
			}
		}

		for (int index = 0; index < spans.length; index++) {
			if (spans[index] != null) {
				continue;
			}

			spans[index] = CDateRange.all();
		}

		return spans;
	}

	/**
	 * Helper method for calculateEntityDateIndices, swapping {@link Integer#MIN_VALUE}/{@link Integer#MAX_VALUE} for performance.
	 */
	private static CDateRange calculateSpan(int max, int min, CDateRange in) {

		if (max == Integer.MIN_VALUE && min == Integer.MAX_VALUE) {
			return in;
		}

		final CDateRange span;

		if (max == Integer.MIN_VALUE) {
			span =  CDateRange.atLeast(min);
		}
		else if (min == Integer.MAX_VALUE) {
			span = CDateRange.atMost(max);
		}
		else {
			span = CDateRange.of(min, max);
		}

		if (in == null){
			return span;
		}

		return in.span(span);
	}


	public <T> Int2ObjectMap<T> getColumnIndex(Column column){
		return (Int2ObjectMap<T>) columnIndices.get(column);
	}

	public static Int2ObjectMap<?> createColumnIndex(Column column, Bucket bucket) {

		final Int2ObjectMap<Object> index = new Int2ObjectAVLTreeMap<>();

		for (int entity : bucket.entities()) {
			final int entityEnd = bucket.getEntityEnd(entity);
			Object entityIndex = index.get(entity);

			for (int event = bucket.getEntityStart(entity); event < entityEnd; event++) {
				if (bucket.has(event, column)){
					entityIndex = indexColumn(bucket, event, column, entityIndex);
				}
			}

			if (entityIndex != null) {
				index.put(entity, entityIndex);
			}
		}

		return index;
	}

	private static Object indexStringColumn(Bucket bucket, int event, Column column, Object current) {

		final int value = bucket.getString(event, column);

		if(current == null) {
			current = new IntOpenHashSet();
		}

		((IntSet) current).add(value);

		return current;
	}

	private static Range<Long> indexIntegerColumn(Bucket bucket, int event, Column column, Object current) {

		final long value = bucket.getInteger(event, column);

		final Range<Long> exactly = Range.LongRange.exactly(value);

		if(current == null){
			return exactly;
		}

		return ((Range<Long>) current).span(exactly);
	}

	private static CDateRange indexDateColumn(Bucket bucket, int event, Column column, Object current) {
		final int value = bucket.getDate(event, column);

		final CDateRange exactly = CDateRange.exactly(value);

		if (current == null) {
			return exactly;
		}

		return ((CDateRange) current).span(exactly);
	}

	private static Object indexColumn(Bucket bucket, int event, Column column, @Nullable Object current) {
		return switch (column.getType()) {
			case STRING -> indexStringColumn(bucket, event, column, current);
			case INTEGER -> indexIntegerColumn(bucket, event, column, current);
			case DATE -> indexDateColumn(bucket, event, column, current);
			default -> null;
		};
	}
}
