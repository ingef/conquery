package com.bakdata.conquery.models.datasets.concepts.select.concept;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;

public abstract class UniversalSelect extends Select {
	@Override
	public List<ColumnId> getRequiredColumns() {
		return Collections.emptyList(); // UniversalSelects usually have no required columns
	}
}
