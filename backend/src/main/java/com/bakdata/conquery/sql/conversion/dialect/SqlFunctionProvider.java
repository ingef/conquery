package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import org.jooq.Condition;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

/**
 * Provider of SQL functions.
 */
public interface SqlFunctionProvider {

	String DEFAULT_DATE_FORMAT = "yyyy-mm-dd";


	Condition dateRestriction(Field<Object> dateRestrictionColumn, Field<Object> validityDateColumn);

	/**
	 * @return A daterange for a date restriction.
	 */
	Field<Object> daterange(CDateRange dateRestriction);

	/**
	 * @return A daterange for an existing column.
	 */
	Field<Object> daterange(Column column);

	default Field<Date> toDate(String dateColumn) {
		return DSL.toDate(dateColumn, DEFAULT_DATE_FORMAT);
	}

	default Field<Integer> dateDistance(DatePart timeUnit, Date endDate, Column startDateColumn) {
		if (startDateColumn.getType() != MajorTypeId.DATE) {
			throw new UnsupportedOperationException("Can't calculate date distance to column of type "
													+ startDateColumn.getType());
		}
		// we can now safely cast to Field of type Date
		Field<Date> startDate = DSL.field(startDateColumn.getName(), Date.class);
		return DSL.dateDiff(timeUnit, startDate, endDate);
	}

	default Condition in(String columnName, String[] values) {
		return DSL.field(columnName)
				  .in(values);
	}

	default Field<Object> first(String columnName) {
		// TODO: this is just a temporary placeholder
		return DSL.field(columnName);
	}

	default TableOnConditionStep<Record> innerJoin(
			Table<Record> leftPartQueryBase,
			QueryStep rightPartQS,
			Field<Object> leftPartPrimaryColumn,
			Field<Object> rightPartPrimaryColumn
	) {
		return leftPartQueryBase
				.innerJoin(DSL.name(rightPartQS.getCteName()))
				.on(leftPartPrimaryColumn.eq(rightPartPrimaryColumn));
	}

	default TableOnConditionStep<Record> fullOuterJoin(
			Table<Record> leftPartQueryBase,
			QueryStep rightPartQS,
			Field<Object> leftPartPrimaryColumn,
			Field<Object> rightPartPrimaryColumn
	) {
		return leftPartQueryBase
				.fullOuterJoin(DSL.name(rightPartQS.getCteName()))
				.on(leftPartPrimaryColumn.eq(rightPartPrimaryColumn));
	}

}
