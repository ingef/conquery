package com.bakdata.conquery.models.events;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "value")
@CPSBase
public interface ColumnStore<T, SELF extends ColumnStore<T, ?  extends ColumnStore<T, SELF>>> {

	SELF merge(List<SELF> stores);

	boolean has(int event);

	T get(int event);

	int getString(int event);

	long getInteger(int event);

	boolean getBoolean(int event);

	double getReal(int event);

	BigDecimal getDecimal(int event);

	long getMoney(int event);

	int getDate(int event);

	CDateRange getDateRange(int event);

	Object getAsObject(int event);

	void serialize(OutputStream outputStream);

}
