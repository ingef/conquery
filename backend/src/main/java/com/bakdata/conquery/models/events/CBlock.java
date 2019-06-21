package com.bakdata.conquery.models.events;

import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.CBlockDeserializer;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@JsonDeserialize(using = CBlockDeserializer.class)
public class CBlock extends IdentifiableImpl<CBlockId> {
	
	@Valid
	private BucketId bucket;
	@NotNull @Valid
	private ConnectorId connector;
	private long[] includedConcepts;
	private int[] minDate;
	private int[] maxDate;
	@Valid
	private List<int[]> mostSpecificChildren;
	
	public CBlock(BucketId bucket, ConnectorId connector) {
		this.bucket = bucket;
		this.connector = connector;
	}
	
	@Override @JsonIgnore
	public CBlockId createId() {
		return new CBlockId(bucket, connector);
	}

	public void initIndizes(int bucketSize) {
		includedConcepts = new long[bucketSize];
		minDate = new int[bucketSize];
		maxDate = new int[bucketSize];
		Arrays.fill(minDate, Integer.MAX_VALUE);
		Arrays.fill(maxDate, Integer.MIN_VALUE);
	}
}
