package com.bakdata.conquery.sql.conversion.model.select;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class DateDistanceSqlSelect implements SqlSelect {

	private final DateNowSupplier dateNowSupplier;
	private final ChronoUnit timeUnit;
	private final String sourceTable;
	private final Column column;
	private final String alias;
	private final CDateRange dateRestriction;
	@EqualsAndHashCode.Exclude
	private final SqlFunctionProvider functionProvider;

	@Override
	public Field<Integer> select() {
		Date endDate = getEndDate(dateRestriction);

		if (column.getType() != MajorTypeId.DATE) {
			throw new UnsupportedOperationException("Can't calculate date distance to column of type " + column.getType());
		}

		Name dateColumnName = DSL.name(sourceTable, column.getName());
		return functionProvider.dateDistance(timeUnit, dateColumnName, endDate)
							   .as(alias);
	}

	private Date getEndDate(CDateRange dateRange) {
		LocalDate endDate;
		// if a date restriction is set, the max of the date restriction equals the end date of the date distance
		// but there is also the possibility that the user set's an empty daterange which will be non-null but with null values
		if (Objects.nonNull(dateRange) && dateRange.getMax() != null) {
			endDate = dateRange.getMax();
		}
		else {
			// otherwise the current date is the upper bound
			endDate = dateNowSupplier.getLocalDateNow();
		}
		return Date.valueOf(endDate);
	}

	@Override
	public Field<Integer> aliased() {
		return DSL.field(alias, Integer.class);
	}

	@Override
	public List<String> columnNames() {
		return List.of(column.getName());
	}

}
