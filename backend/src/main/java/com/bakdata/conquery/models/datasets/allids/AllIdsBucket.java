package com.bakdata.conquery.models.datasets.allids;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.PrimitiveIterator;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

public class AllIdsBucket extends Bucket {

	@Getter
	private final IntList entities = new IntArrayList();

	public AllIdsBucket(Import imp, int bucket, IntList entities) {
		super(bucket, imp, new int[0]);
		this.entities.addAll(entities);
	}

	@Override
	public void initFields(int numberOfEntities) {

	}

	@Override
	public int toGlobal(int entity) {
		return entity;
	}

	@Override
	public int toLocal(int entity) {
		return entity;
	}

	@Override
	public boolean containsLocalEntity(int localEntity) {
		return getBucket() <= localEntity && localEntity <= getBucket() + ConqueryConfig.getInstance().getCluster().getEntityBucketSize();
	}

	@Override
	public PrimitiveIterator.OfInt iterator() {
		return entities.iterator();
	}

	@Override
	public int getFirstEventOfLocal(int localEntity) {
		return 0;
	}

	@Override
	public int getLastEventOfLocal(int localEntity) {
		return 1;
	}

	@Override
	public int getBucketSize() {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public boolean has(int event, int columnPosition) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public int getString(int event, Column column) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public long getInteger(int event, Column column) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public boolean getBoolean(int event, Column column) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public double getReal(int event, Column column) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public BigDecimal getDecimal(int event, Column column) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
public long getMoney(int event, Column column) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
}

	@Override
	public int getDate(int event, Column column) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public CDateRange getDateRange(int event, Column column) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public Object getRaw(int event, int columnPosition) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public Object getAsObject(int event, int columnPosition) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public boolean eventIsContainedIn(int event, Column column, CDateRange dateRange) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public boolean eventIsContainedIn(int event, Column column, CDateSet dateRanges) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public CDateRange getAsDateRange(int event, Column currentColumn) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public Map<String, Object> calculateMap(int event, Import imp) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public void writeContent(Output output) throws IOException {
		for (Integer entity : entities) {
			output.writeInt(entity,true);
		}
	}

	@Override
	public void read(Input input) throws IOException {
		while (input.canReadInt()) {
			entities.add(input.readInt());
		}
	}
}
