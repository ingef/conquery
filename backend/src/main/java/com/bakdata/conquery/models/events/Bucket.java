package com.bakdata.conquery.models.events;

import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.BucketDeserializer;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonDeserialize(using = BucketDeserializer.class)
public class Bucket extends IdentifiableImpl<BucketId> implements Iterable<Integer> {

	@Min(0)
	private int bucket;
	@NotNull @NsIdRef
	private Import imp;
	@NotNull @JsonManagedReference
	private Block[] blocks;
	
	@Override
	public BucketId createId() {
		return new BucketId(imp.getId(), bucket);
	}

	@Override
	public PrimitiveIterator.OfInt iterator() {
		int size = ConqueryConfig.getInstance().getCluster().getEntityBucketSize();
		return IntStream.range(bucket*size, (bucket+1)*size).iterator();
	}

	public int getPosition(int entity) {
		int size = ConqueryConfig.getInstance().getCluster().getEntityBucketSize();
		return entity - bucket*size;
	}
	
	public Block getBlockFor(int entity) {
		return blocks[getPosition(entity)];
	}
}
