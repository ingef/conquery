package com.bakdata.conquery.sql.conversion.select;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;

public class DateDistanceConverter implements SelectConverter<DateDistanceSelect> {

	private final DateNowSupplier dateNowSupplier;

	public DateDistanceConverter(DateNowSupplier dateNowSupplier) {
		this.dateNowSupplier = dateNowSupplier;
	}

	@Override
	public Field<Integer> convert(DateDistanceSelect select, ConversionContext context) {

		ChronoUnit timeUnit = select.getTimeUnit();
		Name startDateColumnName = DSL.name(select.getColumn().getName());
		Date endDate = getEndDate(context);

		return context.getSqlDialect().getFunction().dateDistance(timeUnit, startDateColumnName, endDate)
					  .as(select.getLabel());
	}

	private Date getEndDate(ConversionContext context) {
		LocalDate endDate;
		// if a date restriction is set, the max of the date restriction equals the end date of the date distance
		if (Objects.nonNull(context.getDateRestrictionRange())) {
			endDate = context.getDateRestrictionRange().getMax();
		}
		else {
			// otherwise the current date is the upper bound
			endDate = dateNowSupplier.getLocalDateNow();
		}
		return Date.valueOf(endDate);
	}

	@Override
	public Class<DateDistanceSelect> getConversionClass() {
		return DateDistanceSelect.class;
	}

}
