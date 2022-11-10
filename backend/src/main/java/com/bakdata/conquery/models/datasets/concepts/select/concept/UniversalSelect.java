package com.bakdata.conquery.models.datasets.concepts.select.concept;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;

public abstract class UniversalSelect extends Select {
	@Override
	public List<Column> getRequiredColumns() {
		return Collections.emptyList(); // UniversalSelects usually have no required columns
	}
}
