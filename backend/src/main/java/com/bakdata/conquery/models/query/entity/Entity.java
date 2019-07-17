package com.bakdata.conquery.models.query.entity;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @ToString(of="id")
public class Entity {
	@Getter
	private final int id;
	private final ListMultimap<Table, Bucket> buckets = ArrayListMultimap.create();
	private final HashBasedTable<ConnectorId, BucketId, EntityRow> cBlocks = HashBasedTable.create();

	public void addBucket(Table table, Bucket bucket) {
		buckets.put(table, bucket);
	}
	
	public void addCBlock(Connector con, Import imp, Table table, Bucket bucket, CBlock cBlock) {
		if(cBlocks.put(con.getId(), bucket.getId(), new EntityRow(bucket, cBlock, con, imp, table)) != null) {
			throw new IllegalStateException("multiple CBlocks for block "+bucket+" & connector "+con);
		}
	}

	public void removeBucket(BucketId id) {
		buckets.values().removeIf(b->b.getId().equals(id));
	}

	public void removeCBlock(ConnectorId connector, BucketId bucket) {
		if(cBlocks.remove(connector, bucket) == null) {
			return;
		}
	}

	public static int getBucket(int entityId, int entityBucketSize) {
		return entityId / entityBucketSize;
	}

	public EntityRow getCBlock(ConnectorId connector, BucketId bucket) {
		return cBlocks.get(connector, bucket);
	}
	
	public Map<BucketId, EntityRow> getCBlockPreSelect(ConnectorId connector) {
		return cBlocks.row(connector);
	}

	public List<Bucket> getBucket(Table table) {
		return buckets.get(table);
	}
}
