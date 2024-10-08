package com.bakdata.conquery.sql.conversion.model;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@SuperBuilder
@AllArgsConstructor
public class SqlIdColumns implements Qualifiable<SqlIdColumns> {

	@Getter
	private final Field<Object> primaryColumn;

	@Nullable
	private final Field<Object> secondaryId;

	@Nullable
	private final SqlIdColumns predecessor;

	public SqlIdColumns(Field<Object> primaryColumn, Field<Object> secondaryId) {
		this.primaryColumn = primaryColumn;
		this.secondaryId = secondaryId;
		this.predecessor = null;
	}

	public SqlIdColumns(Field<Object> primaryColumn) {
		this.primaryColumn = primaryColumn;
		this.secondaryId = null;
		this.predecessor = null;
	}

	public SqlIdColumns withAlias() {
		if (this.secondaryId == null) {
			return new SqlIdColumns(this.primaryColumn.as(SharedAliases.PRIMARY_COLUMN.getAlias()), null, this);
		}
		return new SqlIdColumns(
				this.primaryColumn.as(SharedAliases.PRIMARY_COLUMN.getAlias()),
				this.secondaryId.as(SharedAliases.SECONDARY_ID.getAlias()),
				this
		);
	}

	@Override
	public SqlIdColumns qualify(String qualifier) {
		Field<Object> primaryColumn = QualifyingUtil.qualify(this.primaryColumn, qualifier);
		if (secondaryId == null) {
			return new SqlIdColumns(primaryColumn, null, this);
		}
		Field<Object> secondaryId = QualifyingUtil.qualify(this.secondaryId, qualifier);
		return new SqlIdColumns(primaryColumn, secondaryId, this);
	}

	public SqlIdColumns withAbsoluteStratification(Resolution resolution, Field<Integer> index) {
		Field<String> resolutionField = DSL.val(resolution.toString()).as(SharedAliases.RESOLUTION.getAlias());
		return StratificationSqlIdColumns.builder()
										 .primaryColumn(this.primaryColumn)
										 .secondaryId(this.secondaryId)
										 .resolution(resolutionField)
										 .index(index)
										 .eventDate(null)
										 .build();
	}

	public SqlIdColumns withRelativeStratification(Resolution resolution, Field<Integer> index, Field<Date> eventDate) {
		Field<String> resolutionField = DSL.val(resolution.toString()).as(SharedAliases.RESOLUTION.getAlias());
		return StratificationSqlIdColumns.builder()
										 .primaryColumn(this.primaryColumn)
										 .secondaryId(this.secondaryId)
										 .resolution(resolutionField)
										 .index(index)
										 .eventDate(eventDate)
										 .build();
	}

	public SqlIdColumns forFinalSelect() {
		return this;
	}

	public Optional<Field<Object>> getSecondaryId() {
		return Optional.ofNullable(this.secondaryId);
	}

	public Optional<SqlIdColumns> getPredecessor() {
		return Optional.ofNullable(this.predecessor);
	}

	public boolean isWithStratification() {
		return false;
	}

	public List<Field<?>> toFields() {
		return Stream.concat(Stream.of(this.primaryColumn), Optional.ofNullable(this.secondaryId).stream()).collect(Collectors.toList());
	}

	public List<Condition> join(SqlIdColumns rightIds) {

		// always join on primary columns
		Condition joinPrimariesCondition = primaryColumn.eq(rightIds.getPrimaryColumn());

		// join on secondary IDs if both are present
		Condition joinSecondaries = getSecondaryId()
				.flatMap(leftSecondaryId -> rightIds.getSecondaryId().map(leftSecondaryId::eq))
				.orElse(DSL.noCondition());

		return List.of(joinPrimariesCondition, joinSecondaries);
	}

	public SqlIdColumns coalesce(List<SqlIdColumns> selectsIds) {

		List<Field<?>> primaryColumns = new ArrayList<>();
		List<Field<?>> secondaryIds = new ArrayList<>();

		// add this ids
		primaryColumns.add(this.primaryColumn);
		getSecondaryId().ifPresent(secondaryIds::add);

		// add all other ids to coalesce with
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


	protected static Field<Object> coalesceFields(List<Field<?>> fields) {
		if (fields.size() == 1) {
			return fields.get(0).coerce(Object.class);
		}
		return DSL.coalesce(fields.get(0), fields.subList(1, fields.size()).toArray());
	}

}
