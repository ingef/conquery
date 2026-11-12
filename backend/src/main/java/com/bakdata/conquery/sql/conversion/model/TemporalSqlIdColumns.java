package com.bakdata.conquery.sql.conversion.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * Internal ID wrapper which keeps independent temporal evaluation windows apart while nested queries are converted.
 */
final class TemporalSqlIdColumns extends SqlIdColumns {

	private final SqlIdColumns base;
	private final Field<Integer> partition;

	TemporalSqlIdColumns(SqlIdColumns base, Field<Integer> partition) {
		super(base.getPrimaryColumn(), base.getSecondaryId().orElse(null));
		this.base = base;
		this.partition = partition;
	}

	@Override
	public SqlIdColumns qualify(String qualifier) {
		return new TemporalSqlIdColumns(base.qualify(qualifier), QualifyingUtil.qualify(partition, qualifier));
	}

	@Override
	public SqlIdColumns withoutTemporalPartition() {
		return base;
	}

	@Override
	public Optional<Field<Integer>> getTemporalPartition() {
		return Optional.of(partition);
	}

	@Override
	public boolean isWithStratification() {
		return base.isWithStratification();
	}

	@Override
	public SqlIdColumns forFinalSelect() {
		return new TemporalSqlIdColumns(base.forFinalSelect(), partition);
	}

	@Override
	public List<Field<?>> toFields() {
		List<Field<?>> fields = new ArrayList<>(base.toFields());
		fields.add(partition);
		return fields;
	}

	@Override
	public List<Condition> join(SqlIdColumns rightIds) {
		if (!(rightIds instanceof TemporalSqlIdColumns right)) {
			return base.join(rightIds);
		}

		List<Condition> conditions = new ArrayList<>(base.join(right.base));
		conditions.add(partition.eq(right.partition));
		return conditions;
	}

	@Override
	public SqlIdColumns coalesce(List<SqlIdColumns> selectsIds) {
		Preconditions.checkArgument(selectsIds.stream().allMatch(TemporalSqlIdColumns.class::isInstance),
				"Can only coalesce temporal IDs with other temporal IDs");

		List<TemporalSqlIdColumns> temporalIds = selectsIds.stream().map(TemporalSqlIdColumns.class::cast).toList();
		SqlIdColumns coalescedBase = base.coalesce(temporalIds.stream().map(id -> id.base).toList());
		List<Field<Integer>> partitions = new ArrayList<>();
		partitions.add(partition);
		temporalIds.stream().map(id -> id.partition).forEach(partitions::add);
		Field<Integer> coalescedPartition = coalesceFields(partitions, Integer.class).as(partition.getName());
		return new TemporalSqlIdColumns(coalescedBase, coalescedPartition);
	}
}
