package com.bakdata.conquery.sql.compiler;

import java.util.List;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Self-contained result of SQL compilation.
 *
 * <p>The backend executes {@link #sql()} and uses the ordered {@link #columns()} to correlate and decode the JDBC
 * result. Datasource selection, execution, and result post-processing are deliberately excluded.</p>
 */
public record CompiledQuery(
		@NotBlank String sql,
		@NotEmpty List<@NotNull @Valid CompiledColumn> columns
) {

	public CompiledQuery {
		columns = ModelNormalization.immutableCopy(columns);
	}

	@AssertTrue(message = "compiled column outputIds must be unique")
	public boolean isOutputIdsUnique() {
		return columns == null || columns.stream()
				.map(CompiledColumn::outputId)
				.distinct()
				.count() == columns.size();
	}

	@AssertTrue(message = "compiled column SQL aliases must be unique")
	public boolean isSqlAliasesUnique() {
		return columns == null || columns.stream()
				.map(CompiledColumn::sqlAlias)
				.distinct()
				.count() == columns.size();
	}
}
