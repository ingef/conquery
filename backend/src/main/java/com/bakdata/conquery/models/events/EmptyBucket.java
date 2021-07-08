package com.bakdata.conquery.models.events;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import lombok.Getter;

/**
 * This class serves as a not-null placeholder for an empty {@link Bucket}. This avoids verbose null-checking in the QueryEngine.
 */
public class EmptyBucket extends Bucket {

	@Getter
	private static final EmptyBucket Instance = new EmptyBucket();

	public EmptyBucket() {
		super(0, 0, 0, new ColumnStore[0], Collections.emptySet(), new int[0], new int[0], null);
	}


	@Override
	public boolean eventIsContainedIn(int event, Column column, CDateSet dateRanges) {
		return false;
	}

	@Override
	public boolean containsEntity(int entity) {
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
	public CDateRange getAsDateRange(int event, Column column) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

	@Override
	public Map<String, Object> calculateMap(int event) {
		throw new IllegalStateException("Bucket for ALL_IDS_TABLE may not be evaluated.");
	}

}
