package com.bakdata.conquery.models.events;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.CBlockDeserializer;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadata for connection of {@link Bucket} and {@link Concept}
 * <p>
 * Pre-computed assignment of {@link TreeConcept}.
 */
// TODO move to Bucket
@Getter
@Setter
@NoArgsConstructor
@JsonDeserialize(using = CBlockDeserializer.class)
public class CBlock extends IdentifiableImpl<CBlockId> {

	private int root;

	/**
	 * Estimate the memory usage of CBlocks.
	 * @param depthEstimate estimate of depth of mostSpecificChildren
	 */
	public static long estimateMemoryBytes(long entities, long entries, double depthEstimate){
		return Math.round(entities *
						  (
						  		Integer.BYTES + Long.BYTES // includedConcepts
								+ 2 * Integer.BYTES // minDate
								+ 2 * Integer.BYTES // maxDate
						  )
						  + entries * depthEstimate * Integer.BYTES // mostSpecificChildren (rough estimate, not resident on ManagerNode)
		);
	}

	@NsIdRef
	private Bucket bucket;
	@NotNull
	private ConnectorId connector;

	/**
	 * Bloom filter per entity for the first 64 {@link ConceptTreeChild}.
	 */
	private long[] includedConcepts;

	/**
	 * Statistic for fast lookup if entity is of interest.
	 * Int array for memory performance.
	 */
	private int[] minDate;
	private int[] maxDate;

	private int getEntityIndex(int entity){
		return entity - root;
	}

	/**
	 * Represents the path in a {@link TreeConcept} to optimize lookup.
	 * Nodes in the tree are simply enumerated.
	 */
	// todo, can this be implemented using a store or at least with bytes only?
	private int[][] mostSpecificChildren;

	public CBlock(Bucket bucket, ConnectorId connector) {
		this.bucket = bucket;
		this.connector = connector;
	}

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

	public int getEntityMaxDate(int entity) {
		return maxDate[getEntityIndex(entity)];
	}

	@Override
	@JsonIgnore
	public CBlockId createId() {
		return new CBlockId(bucket.getId(), connector);
	}

	public void initIndizes(int bucketSize) {
		root = bucket.getBucket() * bucketSize;

		includedConcepts = new long[bucketSize];

		minDate = new int[bucketSize];
		maxDate = new int[bucketSize];

		Arrays.fill(includedConcepts, 0);
		Arrays.fill(minDate, Integer.MIN_VALUE);
		Arrays.fill(maxDate, Integer.MAX_VALUE);
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
				int min = Math.max(maxDate[index], maxValue);
				maxDate[index]  = min;
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
