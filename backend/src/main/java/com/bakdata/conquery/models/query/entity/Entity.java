package com.bakdata.conquery.models.query.entity;

import java.util.stream.IntStream;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @Getter @ToString(of="id")
public class Entity {

	private final int id;
	private final ListMultimap<Table, Block> blocks = ArrayListMultimap.create();
	private final HashBasedTable<Connector, Block, EntityRow> cBlocks = HashBasedTable.create();

	public void addBlock(Table table, Block block) {
		blocks.put(table, block);
	}
	
	public void addCBlock(Connector con, Import imp, Table table, Block block, CBlock cBlock) {
		if(cBlocks.put(con, block, new EntityRow(block, cBlock, con, imp, table)) != null) {
			throw new IllegalStateException("multiple CBlocks for block "+block+" & connector "+con);
		}
	}

	public void removeBucket(BucketId id) {
		blocks.values().removeIf(b->b.getBucket().getId().equals(id));
	}

	public void removeCBlock(Connector connector, Block block) {

		if(cBlocks.remove(connector, block) == null) {
			return;
		}
	}

	public void removeCBlock(CBlockId id) {

		throw new UnsupportedOperationException();
	}

	public static int getBucket(int entityId, int entityBucketSize) {
		return entityId / entityBucketSize;
	}

	public static Iterable<Integer> iterateBucket(int bucket) {
		return () -> IntStream.range(bucket*100, (bucket+1)*100).iterator();
	}
}
