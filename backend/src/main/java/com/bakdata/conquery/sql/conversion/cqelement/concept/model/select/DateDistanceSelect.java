package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DateDistanceSelect extends ConquerySelect {

	private final DateNowSupplier dateNowSupplier;
	private final ChronoUnit timeUnit;
	private final String sourceTable;
	private final Column column;
	private final CDateRange dateRestriction;
	private final String label;
	private final SqlFunctionProvider functionProvider;

	@Override
	public Field<Integer> select() {
		Date endDate = getEndDate(dateRestriction);

		if (column.getType() != MajorTypeId.DATE) {
			throw new UnsupportedOperationException("Can't calculate date distance to column of type " + column.getType());
		}

		Name dateColumnName = DSL.name(sourceTable, column.getName());
		return functionProvider.dateDistance(timeUnit, dateColumnName, endDate)
							   .as(label);
	}

	private Date getEndDate(CDateRange dateRange) {
		LocalDate endDate;
		// if a date restriction is set, the max of the date restriction equals the end date of the date distance
		if (Objects.nonNull(dateRange)) {
			endDate = dateRange.getMax();
		}
		else {
			// otherwise the current date is the upper bound
			endDate = dateNowSupplier.getLocalDateNow();
		}
		return Date.valueOf(endDate);
	}

	@Override
	public Field<Integer> alias() {
		return DSL.field(label, Integer.class);
	}

}
