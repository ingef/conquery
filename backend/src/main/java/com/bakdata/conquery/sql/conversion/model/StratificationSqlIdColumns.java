package com.bakdata.conquery.sql.conversion.model;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Getter
@SuperBuilder
class StratificationSqlIdColumns extends SqlIdColumns {

	private final Field<String> resolution;

	private final Field<Integer> index;

	/**
	 * Optional event date. Only set for relative forms.
	 */
	@Nullable
	private final Field<Date> eventDate;

	@Override
	public SqlIdColumns qualify(String qualifier) {

		Field<Object> primaryColumn = QualifyingUtil.qualify(getPrimaryColumn(), qualifier);
		Field<String> resolution = QualifyingUtil.qualify(this.resolution, qualifier);
		Field<Integer> index = QualifyingUtil.qualify(this.index, qualifier);
		Field<Date> eventDate = null;
		if (this.eventDate != null) {
			eventDate = QualifyingUtil.qualify(this.eventDate, qualifier);
		}

		return StratificationSqlIdColumns.builder()
										 .primaryColumn(primaryColumn)
										 .secondaryId(null)
										 .resolution(resolution)
										 .index(index)
										 .eventDate(eventDate)
										 .build();
	}

	/**
	 * Replaces the integer value of the {@link Resolution#COMPLETE} with null values.
	 */
	@Override
	public SqlIdColumns forFinalSelect() {

		Field<Integer> withNulledCompleteIndex = DSL.when(
															this.resolution.eq(DSL.val(Resolution.COMPLETE.toString().toUpperCase())),
															DSL.field(DSL.val(null, Integer.class))
													)
													.otherwise(this.index)
													.as(SharedAliases.INDEX.getAlias());

		return StratificationSqlIdColumns.builder()
										 .primaryColumn(getPrimaryColumn())
										 .secondaryId(null)
										 .resolution(this.resolution)
										 .index(withNulledCompleteIndex)
										 .eventDate(this.eventDate)
										 .build();
	}

	@Override
	public boolean isWithStratification() {
		return true;
	}

	@Override
	public List<Field<?>> toFields() {
		return Stream.of(
							 getPrimaryColumn(),
							 this.resolution,
							 this.index,
							 this.eventDate
					 )
					 .filter(Objects::nonNull)
					 .collect(Collectors.toList());
	}

	@Override
	public List<Condition> join(SqlIdColumns rightIds) {

		if (!rightIds.isWithStratification()) {
			return super.join(rightIds);
		}

		StratificationSqlIdColumns rightStratificationIds = (StratificationSqlIdColumns) rightIds;
		Condition joinResolutionAndIndex = this.resolution.eq(rightStratificationIds.getResolution()).and(this.index.eq(rightStratificationIds.getIndex()));

		Condition joinEventDateCondition;
		if (this.eventDate != null) {
			joinEventDateCondition = this.eventDate.eq(rightStratificationIds.getEventDate());
		}
		else {
			joinEventDateCondition = DSL.noCondition();
		}

		return Stream.concat(
							 super.join(rightIds).stream(),
							 Stream.of(joinResolutionAndIndex, joinEventDateCondition)
					 )
					 .toList();
	}

	@Override
	public SqlIdColumns coalesce(List<SqlIdColumns> selectsIds) {

		Preconditions.checkArgument(
				selectsIds.stream().allMatch(SqlIdColumns::isWithStratification),
				"Can only coalesce SqlIdColumns if all are with stratification"
		);

		List<Field<?>> primaryColumns = new ArrayList<>();
		List<Field<?>> resolutions = new ArrayList<>();
		List<Field<?>> indices = new ArrayList<>();
		List<Field<?>> eventDates = new ArrayList<>();

		// add this ids
		primaryColumns.add(getPrimaryColumn());
		resolutions.add(this.resolution);
		indices.add(this.index);
		if (this.eventDate != null) {
			eventDates.add(this.eventDate);
		}

		for (SqlIdColumns ids : selectsIds) {
			StratificationSqlIdColumns stratificationIds = (StratificationSqlIdColumns) ids;
			primaryColumns.add(stratificationIds.getPrimaryColumn());
			resolutions.add(stratificationIds.getResolution());
			indices.add(stratificationIds.getIndex());
			if (stratificationIds.getEventDate() != null) {
				eventDates.add(stratificationIds.getEventDate());
			}
		}

		Field<Object> coalescedPrimaryColumn = coalesceFields(primaryColumns).as(SharedAliases.PRIMARY_COLUMN.getAlias());
		Field<String> coalescedResolutions = coalesceFields(resolutions).coerce(String.class).as(SharedAliases.RESOLUTION.getAlias());
		Field<Integer> coalescedIndices = coalesceFields(indices).coerce(Integer.class).as(SharedAliases.INDEX.getAlias());
		Field<Date> eventDate = null;
		if (!eventDates.isEmpty()) {
			eventDate = coalesceFields(eventDates).coerce(Date.class).as(SharedAliases.INDEX_DATE.getAlias());
		}

		return StratificationSqlIdColumns.builder()
										 .primaryColumn(coalescedPrimaryColumn)
										 .secondaryId(null)
										 .resolution(coalescedResolutions)
										 .index(coalescedIndices)
										 .eventDate(eventDate)
										 .build();
	}
}
