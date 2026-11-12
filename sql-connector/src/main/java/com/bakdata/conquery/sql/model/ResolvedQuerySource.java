package com.bakdata.conquery.sql.model;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import com.bakdata.conquery.sql.model.schema.EntitySchema;

/**
 * A fully resolved query result that can be translated into a reusable SQL relation.
 *
 * <p>Every source exposes canonical entity IDs and may expose validity dates, allowing consumers to compose it as a
 * prerequisite independently of its concrete result shape. IDs are derived from the complete translated relation;
 * subtype-specific shortcuts may be added later when semantic equivalence is covered by tests.</p>
 */
public sealed interface ResolvedQuerySource {

	EntitySchema entitySchema();

	/** A concept or secondary-ID query result. */
	record Standard(@NotNull @Valid ResolvedQuery query) implements ResolvedQuerySource {
		@Override
		public EntitySchema entitySchema() {
			return query.entitySchema();
		}
	}

	/** An absolute, relative, or entity-date form result. */
	record Form(@NotNull @Valid ResolvedFormQuery query) implements ResolvedQuerySource {
		@Override
		public EntitySchema entitySchema() {
			return query.prerequisite().entitySchema();
		}
	}

	/** A row-level table-export result. */
	record TableExport(@NotNull @Valid ResolvedTableExportQuery query) implements ResolvedQuerySource {
		@Override
		public EntitySchema entitySchema() {
			return query.prerequisite().entitySchema();
		}
	}

	/**
	 * A prerequisite-only entity union used for array concept queries.
	 *
	 * <p>It may become the basis of standalone array-query translation in a future change.</p>
	 */
	record Union(
			@NotNull @Valid EntitySchema entitySchema,
			@NotEmpty List<@NotNull @Valid ResolvedQuerySource> children,
			boolean includeValidityDate
	) implements ResolvedQuerySource {

		public Union {
			children = List.copyOf(children);
		}

		@AssertTrue(message = "union children must use the declared entity schema")
		public boolean isChildEntitySchemaConsistent() {
			return entitySchema == null || children == null || children.stream()
					.filter(java.util.Objects::nonNull)
					.allMatch(child -> entitySchema.equals(child.entitySchema()));
		}
	}
}
