package com.bakdata.conquery.sql.compiler.rendering;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SharedAliases;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import org.jooq.Field;

/** Renders a select projection from compiler IR into database-specific physical fields. */
public final class SelectProjectionRenderer {

	private SelectProjectionRenderer() {
	}

	/**
	 * Render the final projection fields.
	 *
	 * @param selects compiler projection state
	 * @param dialect database-specific compiler capabilities
	 * @param validityDateRendering whether validity ranges represent individual or grouped rows
	 */
	public static List<Field<?>> render(
			Selects selects,
			CompilerDialect dialect,
			ValidityDateRendering validityDateRendering
	) {
		Optional<Field<?>> validityDateRendered = selects.getValidityDate()
				.map(dateRange -> renderValidityDate(dateRange, dialect, validityDateRendering)
						.as(SharedAliases.DATES_COLUMN.getAlias()));

		Optional<Field<?>> stratificationDateRendered = selects.getStratificationDate()
				.map(dateRange -> dialect.renderDateRange(dateRange.getStart(), dateRange.getEnd())
						.as(SharedAliases.STRATIFICATION_BOUNDS.getAlias()));

		return Stream.of(
						selects.getIds().toFields().stream(),
						stratificationDateRendered.stream(),
						validityDateRendered.stream(),
						selects.getSqlSelects().stream().flatMap(sqlSelect -> sqlSelect.toFinalRepresentation().toFields().stream())
				)
				.flatMap(Function.identity())
				.map(select -> (Field<?>) select)
				.distinct()
				.collect(Collectors.toList());
	}

	private static Field<?> renderValidityDate(
			ColumnDateRange dateRange,
			CompilerDialect dialect,
			ValidityDateRendering validityDateRendering
	) {
		return switch (validityDateRendering) {
			case AGGREGATED -> dialect.aggregateDateRanges(dateRange.getStart(), dateRange.getEnd());
			case INDIVIDUAL -> dialect.renderDateRange(dateRange.getStart(), dateRange.getEnd());
		};
	}

	/** Physical representation required for validity dates in the projection. */
	public enum ValidityDateRendering {
		AGGREGATED,
		INDIVIDUAL
	}
}
