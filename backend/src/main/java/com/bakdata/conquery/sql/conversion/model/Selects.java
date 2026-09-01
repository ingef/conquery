package com.bakdata.conquery.sql.conversion.model;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jooq.Field;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	public List<Field<?>> toFinalRepresentation(SqlFunctionProvider functionProvider, boolean forTableExport) {

		final Optional<Field<?>> validityDateRendered =
				!forTableExport
						? getValidityDate().map(vdd -> functionProvider.dateRangeAggregation(vdd).as(SharedAliases.DATES_COLUMN.getAlias()))
						: getValidityDate().map(vdd -> functionProvider.dateRangeToField(vdd).as(SharedAliases.DATES_COLUMN.getAlias()));

		final Optional<Field<?>> stratificationDateRendered = getStratificationDate().map(vdd -> functionProvider.dateRangeToField(vdd).as(SharedAliases.STRATIFICATION_BOUNDS.getAlias()));

		return Stream.of(
						getIds().toFields().stream(),
						stratificationDateRendered.stream(),
						validityDateRendered.stream(),
						getSqlSelects().stream().flatMap(sqlSelect -> sqlSelect.toFinalRepresentation().toFields().stream())
				)
				.flatMap(Function.identity())
				.map(select -> (Field<?>) select)
				.distinct()
				.collect(Collectors.toList());
	}

	public Selects blockValidityDate() {
		return this.toBuilder()
				.validityDate(Optional.empty())
				.build();
	}

	public Selects qualify(String qualifier) {
		SqlIdColumns ids = this.ids.qualify(qualifier);
		List<SqlSelect> sqlSelects = this.sqlSelects.stream().map(sqlSelect -> sqlSelect.qualify(qualifier)).collect(Collectors.toList());

		SelectsBuilder builder = Selects.builder()
				.ids(ids)
				.sqlSelects(sqlSelects);

		if (this.validityDate.isPresent()) {
			builder = builder.validityDate(this.validityDate.map(_validityDate -> _validityDate.qualify(qualifier)));
		}

		if (this.stratificationDate.isPresent()) {
			builder = builder.stratificationDate(this.stratificationDate.map(_validityDate -> _validityDate.qualify(qualifier)));
		}

		return builder.build();
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
