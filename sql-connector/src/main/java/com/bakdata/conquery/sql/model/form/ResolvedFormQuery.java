package com.bakdata.conquery.sql.model.form;

import java.util.List;

import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.result.ResultColumn;

public record ResolvedFormQuery(
		ResolvedQuery prerequisite,
		List<ResolvedQuery> features,
		ResolvedFormMode mode,
		List<ResultColumn> resultColumns
) {
	public ResolvedFormQuery {
		features = List.copyOf(features);
		resultColumns = List.copyOf(resultColumns);
	}
}
