package com.bakdata.conquery.sql.conversion.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.bakdata.conquery.sql.conversion.SharedAliases;
import lombok.Getter;
import lombok.NonNull;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class SqlIdColumns implements Qualifiable<SqlIdColumns> {

	@Getter
	private final Field<Object> primaryColumn;

	@Nullable
	private final Field<Object> secondaryId;

	private final List<Field<?>> ids;

	public SqlIdColumns(Field<Object> primaryColumn, @NonNull Field<Object> secondaryId) {
		this.primaryColumn = primaryColumn;
		this.secondaryId = secondaryId;
		this.ids = Stream.concat(Stream.of(this.primaryColumn), Stream.ofNullable(this.secondaryId)).collect(Collectors.toList());
	}

	public SqlIdColumns(Field<Object> primaryColumn) {
		this.primaryColumn = primaryColumn;
		this.secondaryId = null;
		this.ids = List.of(this.primaryColumn);
	}

	@Override
	public SqlIdColumns qualify(String qualifier) {
		Field<Object> primaryColumn = QualifyingUtil.qualify(this.primaryColumn, qualifier);
		if (this.secondaryId == null) {
			return new SqlIdColumns(primaryColumn);
		}
		Field<Object> secondaryId = QualifyingUtil.qualify(this.secondaryId, qualifier);
		return new SqlIdColumns(primaryColumn, secondaryId);
	}

	public Optional<Field<Object>> getSecondaryId() {
		return Optional.ofNullable(this.secondaryId);
	}

	public List<Field<?>> toFields() {
		return this.ids;
	}

	public static List<Condition> join(SqlIdColumns leftIds, SqlIdColumns rightIds) {
		Condition joinPrimariesCondition = leftIds.getPrimaryColumn().eq(rightIds.getPrimaryColumn());
		Condition joinSecondariesCondition;
		if (leftIds.getSecondaryId().isPresent() && rightIds.getSecondaryId().isPresent()) {
			joinSecondariesCondition = leftIds.getSecondaryId().get().eq(rightIds.getSecondaryId().get());
		}
		else {
			joinSecondariesCondition = DSL.noCondition();
		}
		return List.of(joinPrimariesCondition, joinSecondariesCondition);
	}

	public static SqlIdColumns coalesce(List<SqlIdColumns> selectsIds) {

		List<Field<?>> primaryColumns = new ArrayList<>(selectsIds.size());
		List<Field<?>> secondaryIds = new ArrayList<>(selectsIds.size());
		selectsIds.forEach(ids -> {
			primaryColumns.add(ids.getPrimaryColumn());
			ids.getSecondaryId().ifPresent(secondaryIds::add);
		});

		Field<Object> coalescedPrimaryColumn = coalesceFields(primaryColumns).as(SharedAliases.PRIMARY_COLUMN.getAlias());
		if (secondaryIds.isEmpty()) {
			return new SqlIdColumns(coalescedPrimaryColumn);
		}
		Field<Object> coalescedSecondaryIds = coalesceFields(secondaryIds).as(SharedAliases.SECONDARY_ID.getAlias());
		return new SqlIdColumns(coalescedPrimaryColumn, coalescedSecondaryIds);
	}

	private static Field<Object> coalesceFields(List<Field<?>> fields) {
		if (fields.size() == 1) {
			return fields.get(0).coerce(Object.class);
		}
		return DSL.coalesce(fields.get(0), fields.subList(1, fields.size()).toArray());
	}

}
