package com.bakdata.conquery.models.query;

import java.util.List;

import com.bakdata.conquery.io.xodus.WorkerStorageRetrivalDelegate;
import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.entity.Entity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import com.bakdata.conquery.models.events.BucketManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Getter @AllArgsConstructor @RequiredArgsConstructor
@With
public class QueryExecutionContext {

	private Column validityDateColumn;
	@NonNull
	private BitMapCDateSet dateRestriction = BitMapCDateSet.createAll();
	private boolean prettyPrint = true;
	private Connector connector;
	private final WorkerStorageRetrivalDelegate storage;
	private final BucketManager bucketManager;

	public List<Bucket> getEntityBucketsForTable(Entity entity, TableId id) {
		return getStorage().getBucketManager().getEntityBucketsForTable(entity, id);
	}
}