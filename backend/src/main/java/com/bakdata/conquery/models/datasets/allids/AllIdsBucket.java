package com.bakdata.conquery.models.datasets.allids;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.PrimitiveIterator;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

//TODO explain this class!
public class AllIdsBucket extends Bucket {

	public static final AllIdsBucket INSTANCE = new AllIdsBucket();

	public AllIdsBucket() {
		super(0, null, new int[0]);
	}

	@Override
	public void initFields(int numberOfEntities) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public int toGlobal(int entity) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public int toLocal(int entity) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public boolean containsLocalEntity(int localEntity) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public void writeContent(Output output) throws IOException {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public void read(Input input) throws IOException {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public PrimitiveIterator.OfInt iterator() {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public int getFirstEventOfLocal(int localEntity) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public int getLastEventOfLocal(int localEntity) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
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

}
