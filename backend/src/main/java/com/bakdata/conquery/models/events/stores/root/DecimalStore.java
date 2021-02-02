package com.bakdata.conquery.models.events.stores.root;

import java.math.BigDecimal;

public abstract class DecimalStore extends ColumnStore<BigDecimal> {

	public abstract BigDecimal getDecimal(int event);
}
