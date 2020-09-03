package com.bakdata.conquery.models.events;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.ImportColumn;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ColumnStore<T extends ColumnStore<T>> {

	@Getter
	private final ImportColumn column;

	public abstract T merge(List<? extends ColumnStore<?>> stores);

	public abstract boolean has(int event);

	public abstract int getString(int event);

	public abstract long getInteger(int event);

	public abstract boolean getBoolean(int event);

	public abstract double getReal(int event);

	public abstract BigDecimal getDecimal(int event);

	public abstract long getMoney(int event);

	public abstract int getDate(int event);

	public abstract CDateRange getDateRange(int event);

	public abstract Object getAsObject(int event);

	public abstract void serialize(OutputStream outputStream);

}
