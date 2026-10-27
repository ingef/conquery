package com.bakdata.conquery.sql.model.form;

import java.util.List;
import java.util.Objects;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.result.ResultColumn;

public record ResolvedFormQuery(
		@NotNull @Valid ResolvedQuery prerequisite,
		@NotEmpty List<@NotNull @Valid ResolvedQuery> features,
		@NotNull @Valid ResolvedFormMode mode,
		@NotNull List<@NotNull @Valid ResultColumn> resultColumns
) {
	public ResolvedFormQuery {
		features = ModelNormalization.immutableCopy(features);
		resultColumns = ModelNormalization.immutableCopy(resultColumns);
	}

	@AssertTrue(message = "result column outputIds must be unique")
	public boolean isResultColumnOutputIdsUnique() {
		return resultColumns == null || resultColumns.stream().anyMatch(Objects::isNull) || resultColumns.stream()
				.map(ResultColumn::outputId)
				.distinct()
				.count() == resultColumns.size();
	}
}
