package com.bakdata.conquery.models.events;

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.CBlockDeserializer;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Metadata for connection of {@link Bucket} and {@link Concept}
 * <p>
 * Pre-computed assignment of {@link TreeConcept}.
 */
// TODO move to Bucket
@Getter
@Setter
@JsonDeserialize(using = CBlockDeserializer.class)
@RequiredArgsConstructor
public class CBlock extends IdentifiableImpl<CBlockId> {

	/**
	 * Estimate the memory usage of CBlocks.
	 * @param depthEstimate estimate of depth of mostSpecificChildren
	 */
	public static long estimateMemoryBytes(long entities, long entries, double depthEstimate){
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
	private final Connector connector;

	/**
	 * We leverage the fact that a Bucket contains entities from bucketSize * {@link Bucket#getBucket()} to (1 + bucketSize) * {@link Bucket#getBucket()} - 1 to layout our internal structure.
	 * This is maps the first Entities entry in this bucket to 0.
	 */
	private final int root;

	/**
	 * Bloom filter per entity for the first 64 {@link ConceptTreeChild}.
	 */
	private final long[] includedConcepts;

	/**
	 * Statistic for fast lookup if entity is of interest.
	 * Int array for memory performance.
	 */
	private final int[] minDate;
	private final int[] maxDate;

	public static CBlock createCBlock(Connector connector, Bucket bucket, int bucketSize) {
		int root = bucket.getBucket() * bucketSize;

		long[] includedConcepts = new long[bucketSize];

		int[] minDate = new int[bucketSize];
		int[] maxDate = new int[bucketSize];

		Arrays.fill(includedConcepts, 0);
		Arrays.fill(minDate, Integer.MIN_VALUE);
		Arrays.fill(maxDate, Integer.MAX_VALUE);

		return new CBlock(bucket, connector, root, includedConcepts, minDate, maxDate);
	}


	/**
	 * Represents the path in a {@link TreeConcept} to optimize lookup.
	 * Nodes in the tree are simply enumerated.
	 */
	// todo, can this be implemented using a store or at least with bytes only?
	private int[][] mostSpecificChildren;


	public static long calculateBitMask(List<ConceptElement<?>> concepts) {
		long mask = 0;
		for (ConceptElement<?> concept : concepts) {
			mask |= concept.calculateBitMask();
		}
		return mask;
	}


	public int[] getEventMostSpecificChild(int event) {
		if(mostSpecificChildren == null){
			return null;
		}

		return mostSpecificChildren[event];
	}

	public CDateRange getEntityDateRange(int entity) {
		return CDateRange.of(getEntityMinDate(entity), getEntityMaxDate(entity));
	}

	public int getEntityMinDate(int entity) {
		return minDate[getEntityIndex(entity)];
	}

	/**
	 * calculate the offset of the entity into this CBlock.
	 * @see this#root
	 */
	private int getEntityIndex(int entity) {
		Preconditions.checkArgument(entity >=  root, "Entity is not of this CBlock.");
		return entity - root;
	}

	public int getEntityMaxDate(int entity) {
		return maxDate[getEntityIndex(entity)];
	}

	@Override
	@JsonIgnore
	public CBlockId createId() {
		return new CBlockId(bucket.getId(), connector.getId());
	}


	public void addEntityDateRange(int entity, CDateRange range) {
		final int index = getEntityIndex(entity);

		if (range.hasLowerBound()) {
			final int minValue = range.getMinValue();

			if (minDate[index] == Integer.MIN_VALUE) {
				minDate[index] = minValue;
			}
			else {
				int min = Math.min(minDate[index], minValue);
				minDate[index] = min;
			}
		}

		if (range.hasUpperBound()) {
			final int maxValue = range.getMaxValue();

			if (maxDate[index] != Integer.MAX_VALUE) {
				maxDate[index] = maxValue;
			}
			else {
				int max = Math.max(maxDate[index], maxValue);
				maxDate[index]  = max;
			}
		}
	}

	public void addIncludedConcept(int entity, ConceptTreeNode<?> node) {
		final int index = getEntityIndex(entity);

		final long mask = node.calculateBitMask();
		final long original = includedConcepts[index];

		includedConcepts[index] = original | mask;
	}

	public boolean isConceptIncluded(int entity, long requiredBits) {
		if (requiredBits == 0L) {
			return true;
		}

		long bits = includedConcepts[getEntityIndex(entity)];

		return (bits & requiredBits) != 0L;
	}
}
