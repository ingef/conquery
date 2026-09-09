package com.bakdata.conquery.sql.compiler.ir;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jooq.Field;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fields selected by a compiler step before dialect-specific rendering.
 *
 * <p>This type only describes and transforms compiler state. Turning date ranges and select expressions into the
 * final SQL projection belongs to the compiler layer that owns the dialect.</p>
 */
@Value
@Builder(toBuilder = true)
public class Selects {

	SqlIdColumns ids;
	@Builder.Default
	Optional<ColumnDateRange> validityDate = Optional.empty();
	@Builder.Default
	Optional<ColumnDateRange> stratificationDate = Optional.empty();
	@Singular
	List<SqlSelect> sqlSelects;

	public Selects blockValidityDate() {
		return this.toBuilder()
				.validityDate(Optional.empty())
				.build();
	}

	public Selects qualify(String qualifier) {
		SqlIdColumns qualifiedIds = this.ids.qualify(qualifier);
		List<SqlSelect> qualifiedSqlSelects = this.sqlSelects.stream()
				.map(sqlSelect -> sqlSelect.qualify(qualifier))
				.toList();

		return Selects.builder()
				.ids(qualifiedIds)
				.validityDate(this.validityDate.map(dateRange -> dateRange.qualify(qualifier)))
				.stratificationDate(this.stratificationDate.map(dateRange -> dateRange.qualify(qualifier)))
				.sqlSelects(qualifiedSqlSelects)
				.build();
	}

	public List<Field<?>> all() {
		return Stream.of(
						this.ids.toFields().stream(),
						this.stratificationDate.stream().flatMap(range -> range.toFields().stream()),
						this.validityDate.stream().flatMap(range -> range.toFields().stream()),
						this.sqlSelects.stream().flatMap(sqlSelect -> sqlSelect.toFields().stream())
				)
				.flatMap(Function.identity())
				.map(select -> (Field<?>) select)
				.distinct()
				.collect(Collectors.toList());
	}

	/**
	 * All fields that have not been explicitly selected (IDs, validity/stratification dates).
	 */
	public List<Field<?>> nonExplicitSelects() {
		return Stream.of(
						this.ids.toFields().stream(),
						this.stratificationDate.stream().flatMap(range -> range.toFields().stream()),
						this.validityDate.stream().flatMap(range -> range.toFields().stream())
				)
				.flatMap(Function.identity())
				.map(select -> (Field<?>) select)
				.distinct()
				.collect(Collectors.toList());
	}

	/**
	 * All explicitly selected and converted fields.
	 */
	public List<Field<?>> explicitSelects() {
		return this.sqlSelects.stream()
				.flatMap(sqlSelect -> sqlSelect.toFields().stream())
				.distinct()
				.collect(Collectors.toList());
	}

}
