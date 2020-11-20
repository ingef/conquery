package com.bakdata.conquery.models.events.generation;

import java.math.BigDecimal;
import java.util.Map;

import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;

/**
 * This class serves as a not-null placeholder for an empty {@link Bucket}. This avoids verbose null-checking in the QueryEngine.
 */
public class EmptyBucket extends Bucket {

	@Getter
	private static final EmptyBucket Instance = new EmptyBucket();

	private EmptyBucket() {
		super(0, null, 0, new CType<?>[0], Map.of(), Map.of(), 0);
	}


	@Override
	public boolean eventIsContainedIn(int event, Column column, BitMapCDateSet dateRanges) {
		return false;
	}

	@Override
	public boolean containsEntity(int localEntity) {
		return false;
	}


	@Override
	public int getEntityStart(int entityId) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public int getEntityEnd(int entityId) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}


	@Override
	public int getBucketSize() {
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
	public CDateRange getAsDateRange(int event, Column currentColumn) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public Map<String, Object> calculateMap(int event) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

}
