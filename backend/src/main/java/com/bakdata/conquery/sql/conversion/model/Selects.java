package com.bakdata.conquery.sql.conversion.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
public class Selects {

	Field<Object> primaryColumn;
	Optional<ColumnDateRange> validityDate;
	List<SqlSelect> sqlSelects;

	public Selects(Field<Object> primaryColumn, Optional<ColumnDateRange> validityDate, List<SqlSelect> sqlSelects) {
		this.primaryColumn = primaryColumn;
		this.validityDate = validityDate;
		this.sqlSelects = sqlSelects;
	}

	/**
	 * {@link Selects} without a validity date.
	 */
	public Selects(Field<Object> primaryColumn, List<SqlSelect> sqlSelects) {
		this.primaryColumn = primaryColumn;
		this.validityDate = Optional.empty();
		this.sqlSelects = sqlSelects;
	}

	/**
	 * {@link Selects#Selects(Field, Optional, List)} qualified onto the given qualifier.
	 */
	public static Selects qualified(String qualifier, Field<Object> primaryColumn, Optional<ColumnDateRange> validityDate, List<SqlSelect> sqlSelects) {
		return new Selects(primaryColumn, validityDate, sqlSelects).qualify(qualifier);
	}

	/**
	 * {@link Selects#Selects(Field, List)} qualified onto the given qualifier.
	 */
	public static Selects qualified(String qualifier, Field<Object> primaryColumn, List<SqlSelect> sqlSelects) {
		return new Selects(primaryColumn, sqlSelects).qualify(qualifier);
	}

	public Selects withValidityDate(ColumnDateRange validityDate) {
		return new Selects(this.primaryColumn, Optional.of(validityDate), this.sqlSelects);
	}

	public Selects blockValidityDate() {
		return new Selects(this.primaryColumn, this.sqlSelects);
	}

	public Selects qualify(String qualifier) {
		Field<Object> qualifiedPrimaryColumn = DSL.field(DSL.name(qualifier, this.primaryColumn.getName()));
		List<SqlSelect> qualifiedSelects = this.sqlSelects.stream()
														  .flatMap(sqlSelect -> sqlSelect.createReferences(qualifier, SqlSelect.class).stream())
														  .distinct()
														  .collect(Collectors.toList());
		if (this.validityDate.isEmpty()) {
			return new Selects(qualifiedPrimaryColumn, qualifiedSelects);
		}
		else {
			return new Selects(qualifiedPrimaryColumn, this.validityDate.map(_validityDate -> _validityDate.qualify(qualifier)), qualifiedSelects);
		}
	}

	public List<Field<?>> all() {
		return Stream.of(
							 Stream.of(this.primaryColumn),
							 this.validityDate.stream().flatMap(range -> range.toFields().stream()),
							 this.sqlSelects.stream().map(SqlSelect::select)
					 )
					 .flatMap(Function.identity())
					 .map(select -> (Field<?>) select)
					 .collect(Collectors.toList());
	}

	public List<Field<?>> explicitSelects() {
		return this.sqlSelects.stream()
							  .map(SqlSelect::select)
							  .collect(Collectors.toList());
	}

}
