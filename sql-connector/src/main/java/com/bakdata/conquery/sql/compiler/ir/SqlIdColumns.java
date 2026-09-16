package com.bakdata.conquery.sql.compiler.ir;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Compiler IR for the columns identifying an entity. */
public class SqlIdColumns implements Qualifiable<SqlIdColumns> {

	private final Field<String> primaryColumn;
	private final Field<String> secondaryId;
	private final SqlIdColumns predecessor;

	public SqlIdColumns(Field<String> primaryColumn, Field<String> secondaryId) {
		this(primaryColumn, secondaryId, null);
	}

	public SqlIdColumns(Field<String> primaryColumn) {
		this(primaryColumn, null, null);
	}

	protected SqlIdColumns(Field<String> primaryColumn, Field<String> secondaryId, SqlIdColumns predecessor) {
		this.primaryColumn = primaryColumn;
		this.secondaryId = secondaryId;
		this.predecessor = predecessor;
	}

	public Field<String> getPrimaryColumn() {
		return primaryColumn;
	}

	public SqlIdColumns withAlias() {
		Field<String> primary = primaryColumn.as(SharedAliases.PRIMARY_COLUMN.getAlias());
		if (secondaryId == null) {
			return new SqlIdColumns(primary, null, this);
		}
		return new SqlIdColumns(primary, secondaryId.as(SharedAliases.SECONDARY_ID.getAlias()), this);
	}

	@Override
	public SqlIdColumns qualify(String qualifier) {
		Field<String> primary = QualifyingUtil.qualify(primaryColumn, qualifier);
		if (secondaryId == null) {
			return new SqlIdColumns(primary, null, this);
		}
		return new SqlIdColumns(primary, QualifyingUtil.qualify(secondaryId, qualifier), this);
	}

	public SqlIdColumns withStratification(String resolution, Field<Integer> index) {
		return withStratification(resolution, index, null);
	}

	/**
	 * Add stratification columns using a backend-independent resolution token.
	 *
	 * @param resolution normalized resolution value supplied at the compiler boundary
	 * @param index stratification index
	 * @param eventDate optional event date used by relative stratification
	 */
	public SqlIdColumns withStratification(String resolution, Field<Integer> index, Field<Date> eventDate) {
		Field<String> resolutionField = DSL.inline(resolution).as(SharedAliases.RESOLUTION.getAlias());
		return new StratificationSqlIdColumns(primaryColumn, resolutionField, index, eventDate);
	}

	/** Convert stratified IDs into their final output form using the normalized complete-resolution token. */
	public SqlIdColumns forFinalSelect(String completeResolution) {
		return this;
	}

	public Optional<Field<String>> getSecondaryId() {
		return Optional.ofNullable(secondaryId);
	}

	public Optional<SqlIdColumns> getPredecessor() {
		return Optional.ofNullable(predecessor);
	}

	public boolean isWithStratification() {
		return false;
	}

	public List<Field<?>> toFields() {
		if (secondaryId == null) {
			return List.of(primaryColumn);
		}
		return List.of(primaryColumn, secondaryId);
	}

	public List<Condition> join(SqlIdColumns rightIds) {
		Condition joinPrimaries = primaryColumn.eq(rightIds.primaryColumn);
		Condition joinSecondaries = getSecondaryId()
				.flatMap(leftSecondary -> rightIds.getSecondaryId().map(leftSecondary::eq))
				.orElse(DSL.noCondition());
		return List.of(joinPrimaries, joinSecondaries);
	}

	public SqlIdColumns coalesce(List<SqlIdColumns> selectsIds) {
		List<Field<String>> primaryColumns = new ArrayList<>();
		List<Field<String>> secondaryIds = new ArrayList<>();

		primaryColumns.add(primaryColumn);
		getSecondaryId().ifPresent(secondaryIds::add);
		selectsIds.forEach(ids -> {
			primaryColumns.add(ids.primaryColumn);
			ids.getSecondaryId().ifPresent(secondaryIds::add);
		});

		Field<String> coalescedPrimary = coalesceFields(primaryColumns, String.class)
				.coerce(String.class)
				.as(SharedAliases.PRIMARY_COLUMN.getAlias());
		if (secondaryIds.isEmpty()) {
			return new SqlIdColumns(coalescedPrimary);
		}
		Field<String> coalescedSecondary = coalesceFields(secondaryIds, String.class)
				.coerce(String.class)
				.as(SharedAliases.SECONDARY_ID.getAlias());
		return new SqlIdColumns(coalescedPrimary, coalescedSecondary);
	}

	protected static <T> Field<T> coalesceFields(List<? extends Field<?>> fields, Class<T> type) {
		Field<T> result = fields.getFirst().coerce(type);
		for (int index = 1; index < fields.size(); index++) {
			result = DSL.coalesce(result, fields.get(index).coerce(type));
		}
		return result;
	}

	@Override
	public String toString() {
		return "SqlIdColumns(primaryColumn=%s, secondaryId=%s)".formatted(primaryColumn, secondaryId);
	}
}
