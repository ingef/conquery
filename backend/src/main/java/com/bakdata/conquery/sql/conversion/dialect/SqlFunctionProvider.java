package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

/**
 * Provider of SQL functions.
 */
public interface SqlFunctionProvider {

	String DEFAULT_DATE_FORMAT = "yyyy-mm-dd";

	/**
	 * A date restriction condition is true if holds:
	 * dateRestrictionStart <= validityDateEnd and dateRestrictionEnd >= validityDateStart
	 */
	Condition dateRestriction(ColumnDateRange dateRestrictionRange, ColumnDateRange validityFieldRange);

	ColumnDateRange daterange(CDateRange dateRestriction);

	ColumnDateRange daterange(ValidityDate validityDate, String conceptLabel);

	Field<Object> daterangeString(ColumnDateRange columnDateRange);

	Field<Integer> dateDistance(ChronoUnit datePart, Column startDateColumn, Date endDateExpression);

	default Condition in(Name columnName, String[] values) {
		return DSL.field(columnName)
				  .in(values);
	}

	default Field<Object> first(Name columnName) {
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

	default Field<Date> toDate(String dateExpression) {
		return DSL.toDate(dateExpression, DEFAULT_DATE_FORMAT);
	}

}
