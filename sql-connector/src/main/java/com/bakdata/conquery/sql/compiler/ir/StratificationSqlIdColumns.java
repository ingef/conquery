package com.bakdata.conquery.sql.compiler.ir;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/** Stratified ID-column representation used internally by the compiler. */
final class StratificationSqlIdColumns extends SqlIdColumns {

	private final Field<String> resolution;
	private final Field<Integer> index;
	private final Field<Date> eventDate;

	StratificationSqlIdColumns(Field<String> primaryColumn, Field<String> resolution, Field<Integer> index, Field<Date> eventDate) {
		super(primaryColumn);
		this.resolution = resolution;
		this.index = index;
		this.eventDate = eventDate;
	}

	@Override
	public SqlIdColumns qualify(String qualifier) {
		Field<Date> qualifiedEventDate = eventDate == null ? null : QualifyingUtil.qualify(eventDate, qualifier);
		return new StratificationSqlIdColumns(
				QualifyingUtil.qualify(getPrimaryColumn(), qualifier),
				QualifyingUtil.qualify(resolution, qualifier),
				QualifyingUtil.qualify(index, qualifier),
				qualifiedEventDate
		);
	}

	@Override
	public SqlIdColumns forFinalSelect(String completeResolution) {
		Field<Integer> finalIndex = DSL.when(
				resolution.eq(DSL.inline(completeResolution)),
				DSL.inline(null, Integer.class)
		)
				.otherwise(index)
				.as(SharedAliases.INDEX.getAlias());
		return new StratificationSqlIdColumns(getPrimaryColumn(), resolution, finalIndex, eventDate);
	}

	@Override
	public boolean isWithStratification() {
		return true;
	}

	@Override
	public List<Field<?>> toFields() {
		return Stream.<Field<?>>of(getPrimaryColumn(), resolution, index, eventDate)
				.filter(Objects::nonNull)
				.toList();
	}

	@Override
	public List<Condition> join(SqlIdColumns rightIds) {
		if (!(rightIds instanceof StratificationSqlIdColumns right)) {
			return super.join(rightIds);
		}

		Condition joinResolutionAndIndex = resolution.eq(right.resolution).and(index.eq(right.index));
		Condition joinEventDate = eventDate == null ? DSL.noCondition() : eventDate.eq(right.eventDate);
		return Stream.concat(super.join(rightIds).stream(), Stream.of(joinResolutionAndIndex, joinEventDate)).toList();
	}

	@Override
	public SqlIdColumns coalesce(List<SqlIdColumns> selectsIds) {
		if (!selectsIds.stream().allMatch(StratificationSqlIdColumns.class::isInstance)) {
			throw new IllegalArgumentException("Can only coalesce SqlIdColumns if all are with stratification");
		}

		List<Field<String>> primaryColumns = new ArrayList<>();
		List<Field<String>> resolutions = new ArrayList<>();
		List<Field<Integer>> indices = new ArrayList<>();
		List<Field<?>> eventDates = new ArrayList<>();

		primaryColumns.add(getPrimaryColumn());
		resolutions.add(resolution);
		indices.add(index);
		if (eventDate != null) {
			eventDates.add(eventDate);
		}

		for (SqlIdColumns ids : selectsIds) {
			StratificationSqlIdColumns stratified = (StratificationSqlIdColumns) ids;
			primaryColumns.add(stratified.getPrimaryColumn());
			resolutions.add(stratified.resolution);
			indices.add(stratified.index);
			if (stratified.eventDate != null) {
				eventDates.add(stratified.eventDate);
			}
		}

		Field<String> primary = coalesceFields(primaryColumns, String.class)
				.coerce(String.class)
				.as(SharedAliases.PRIMARY_COLUMN.getAlias());
		Field<String> coalescedResolution = coalesceFields(resolutions, String.class)
				.as(SharedAliases.RESOLUTION.getAlias());
		Field<Integer> coalescedIndex = coalesceFields(indices, Integer.class)
				.as(SharedAliases.INDEX.getAlias());
		Field<Date> coalescedEventDate = eventDates.isEmpty()
				? null
				: coalesceFields(eventDates, Date.class).as(SharedAliases.INDEX_SELECTOR.getAlias());

		return new StratificationSqlIdColumns(primary, coalescedResolution, coalescedIndex, coalescedEventDate);
	}
}
