package com.bakdata.conquery.models.events;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.CBlockDeserializer;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadata for connection of {@link Bucket} and {@link Concept}
 *
 * Pre-computed assignment of {@link TreeConcept}.
 */
// TODO move to Bucket
@Getter @Setter @NoArgsConstructor
@JsonDeserialize(using = CBlockDeserializer.class)
public class CBlock extends IdentifiableImpl<CBlockId> {

	private BucketId bucket;
	@NotNull
	private ConnectorId connector;
	
	/**
	 * Bloom filter per entity for the first 64 {@link ConceptTreeChild}.
	 */
	private Int2LongArrayMap includedConcepts = new Int2LongArrayMap();
	
	/**
	 * Statistic for fast lookup if entity is of interest.
	 * Int array for memory performance.
	 */
	private Int2IntMap minDate = new Int2IntArrayMap();
	private Int2IntMap maxDate = new Int2IntArrayMap();
	
	/**
	 * Represents the path in a {@link TreeConcept} to optimize lookup.
	 * Nodes in the tree are simply enumerated.
	 */
	// todo, can this be implemented using a store or at least with bytes only?
	private int[][] mostSpecificChildren;
	
	public CBlock(BucketId bucket, ConnectorId connector) {
		this.bucket = bucket;
		this.connector = connector;
	}
	
	@Override @JsonIgnore
	public CBlockId createId() {
		return new CBlockId(bucket, connector);
	}

	public void initIndizes(int bucketSize) {
		includedConcepts = new Int2LongArrayMap();
		includedConcepts.defaultReturnValue(0);

		minDate = new Int2IntArrayMap();

		maxDate = new Int2IntArrayMap();
	}
}
