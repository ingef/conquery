package com.bakdata.conquery.sql.model.node;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** A resolved upload of entity IDs, optional validity dates, and additional result columns. */
public record ExternalNode(
		@NotEmpty List<@NotNull @Valid ExternalEntity> entities,
		@NotNull List<@NotBlank String> valueColumns
) implements QueryNode {

	public ExternalNode {
		entities = ModelNormalization.immutableCopy(entities);
		valueColumns = ModelNormalization.immutableCopy(valueColumns);
	}

	@AssertTrue(message = "external entity IDs must be unique")
	public boolean isEntityIdsUnique() {
		return entities == null || entities.stream()
				.filter(Objects::nonNull)
				.map(ExternalEntity::entityId)
				.distinct()
				.count() == entities.size();
	}

	@AssertTrue(message = "external value column names must be unique")
	public boolean isValueColumnsUnique() {
		return valueColumns == null || new HashSet<>(valueColumns).size() == valueColumns.size();
	}

	@AssertTrue(message = "external entity values must reference declared value columns")
	public boolean isEntityValueColumnsDeclared() {
		if (entities == null || valueColumns == null) {
			return true;
		}
		Set<String> declaredColumns = new HashSet<>(valueColumns);
		return entities.stream()
				.filter(entity -> entity != null && entity.values() != null)
				.flatMap(entity -> entity.values().keySet().stream())
				.allMatch(declaredColumns::contains);
	}
}
