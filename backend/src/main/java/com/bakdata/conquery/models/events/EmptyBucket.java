package com.bakdata.conquery.models.events;

import java.math.BigDecimal;
import java.util.Map;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import lombok.Getter;

/**
 * This class serves as a not-null placeholder for an empty {@link Bucket}. This avoids verbose null-checking in the QueryEngine.
 */
public class EmptyBucket extends Bucket {

	@Getter
	private static final EmptyBucket Instance = new EmptyBucket();

	public EmptyBucket() {
		super(0, Object2IntMaps.emptyMap(), Object2IntMaps.emptyMap(), 0, null);
		this.setStores(new ColumnStore[0]);
	}


	@Override
	public boolean eventIsContainedIn(int event, ValidityDate column, CDateSet dateRanges) {
		return false;
	}

	@Override
	public boolean containsEntity(String entity) {
		return false;
	}


	@Override
	public int getEntityStart(String entityId) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}

	@Override
	public int getEntityEnd(String entityId) {
		throw new IllegalStateException("ALL_IDS Bucket does not do anything");
	}


	@Override
	public String getString(int event, Column column) {
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
