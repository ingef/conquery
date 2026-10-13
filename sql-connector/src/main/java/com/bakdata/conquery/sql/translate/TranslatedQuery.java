package com.bakdata.conquery.sql.translate;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;

/**
 * Self-contained result of SQL compilation.
 *
 * <p>The backend executes {@link #sql()} and uses the ordered {@link #columns()} to correlate and decode the JDBC
 * result. Datasource selection, execution, and result post-processing are deliberately excluded.</p>
 */
public record TranslatedQuery(
		@NotBlank String sql,
		@NotEmpty List<@NotNull @Valid TranslatedColumn> columns
) {

	public TranslatedQuery {
		columns = ModelNormalization.immutableCopy(columns);
	}

	@AssertTrue(message = "compiled column outputIds must be unique")
	public boolean isOutputIdsUnique() {
		return columns == null || columns.stream()
				.map(TranslatedColumn::outputId)
				.distinct()
				.count() == columns.size();
	}

	@AssertTrue(message = "compiled column SQL aliases must be unique")
	public boolean isSqlAliasesUnique() {
		return columns == null || columns.stream()
				.map(TranslatedColumn::sqlAlias)
				.distinct()
				.count() == columns.size();
	}
}
