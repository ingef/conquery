package com.bakdata.conquery.models.events;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class BucketBlock extends IdentifiableImpl<BucketId> {

	@Min(0)
	private int bucket;
	@NotNull @JsonManagedReference
	private Block[] blocks;
	@NotNull @NsIdRef
	private Import imp;
	
	@Override
	public BucketId createId() {
		return new BucketId(imp.getId(), bucket);
	}

}
