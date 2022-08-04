package com.bakdata.conquery.models.datasets.concepts.select.concept;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import org.jetbrains.annotations.Nullable;

public abstract class UniversalSelect extends Select {
	@Nullable
	@Override
	public Column[] getRequiredColumns() {
		return new Column[0]; // UniversalSelects usually have no required columns
	}
}
