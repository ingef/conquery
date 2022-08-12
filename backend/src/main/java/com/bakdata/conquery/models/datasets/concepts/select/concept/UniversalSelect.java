package com.bakdata.conquery.models.datasets.concepts.select.concept;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;

public abstract class UniversalSelect extends Select {
	@Override
	public Column[] getRequiredColumns() {
		return new Column[0]; // UniversalSelects usually have no required columns
	}
}
