package com.bakdata.conquery.models.events.stores.root;

import java.math.BigDecimal;

public interface DecimalStore extends ColumnStore {

	BigDecimal getDecimal(int event);
}
