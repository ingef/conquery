package com.bakdata.conquery.models.events;

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.CBlockDeserializer;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
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
	private final CDateRange[] entitySpan;

	public static CBlock createCBlock(Connector connector, Bucket bucket, int bucketSize) {
		int root = bucket.getBucket() * bucketSize;

		long[] includedConcepts = new long[bucketSize];
		CDateRange[] spans = new CDateRange[bucketSize];

		Arrays.fill(includedConcepts, 0);

		Arrays.fill(spans, CDateRange.all());

		return new CBlock(bucket, connector, root, includedConcepts, spans);
	}


	/**
	 * Per event: represents the path in a {@link TreeConcept} to optimize lookup.
	 * Nodes in the tree are simply enumerated.
	 */
	private int[][] mostSpecificChildren;


	public static long calculateBitMask(List<ConceptElement<?>> concepts) {
		long mask = 0;
		for (ConceptElement<?> concept : concepts) {
			mask |= concept.calculateBitMask();
		}
		return mask;
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


	public void addEntityDateRange(int entity, CDateRange range) {
		final int index = bucket.getEntityIndex(entity);

		entitySpan[index] = entitySpan[index].spanClosed(range);
	}

	public void addIncludedConcept(int entity, ConceptTreeNode<?> node) {
		final int index = bucket.getEntityIndex(entity);

		final long mask = node.calculateBitMask();
		final long original = includedConcepts[index];

		includedConcepts[index] = original | mask;
	}

	public boolean isConceptIncluded(int entity, long requiredBits) {
		if (requiredBits == 0L) {
			return true;
		}

		final int index = bucket.getEntityIndex(entity);

		long bits = includedConcepts[index];

		return (bits & requiredBits) != 0L;
	}

	@Override
	@JsonIgnore
	public Dataset getDataset() {
		return bucket.getDataset();
	}
}
