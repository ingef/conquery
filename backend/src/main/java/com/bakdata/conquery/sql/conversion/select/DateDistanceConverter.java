package com.bakdata.conquery.sql.conversion.select;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class DateDistanceConverter implements SelectConverter<DateDistanceSelect> {

	private static final Map<ChronoUnit, DatePart> DATE_CONVERSION = Map.of(
			ChronoUnit.DECADES, DatePart.DECADE,
			ChronoUnit.YEARS, DatePart.YEAR,
			ChronoUnit.DAYS, DatePart.DAY,
			ChronoUnit.MONTHS, DatePart.MONTH,
			ChronoUnit.CENTURIES, DatePart.CENTURY
	);
	private final Supplier<LocalDate> dateNowSupplier;

	public DateDistanceConverter(Supplier<LocalDate> dateNowSupplier) {
		this.dateNowSupplier = dateNowSupplier;
	}

	@Override
	public Field<Integer> convert(DateDistanceSelect select, ConversionContext context) {
		DatePart timeUnit = DATE_CONVERSION.get(select.getTimeUnit());
		if (timeUnit == null) {
			throw new UnsupportedOperationException("Chrono unit %s is not supported".formatted(select.getTimeUnit()));
		}
		Column startDateColumn = select.getColumn();
		Date endDate = getEndDate(context);

		if (startDateColumn.getType() != MajorTypeId.DATE) {
			throw new UnsupportedOperationException("Can't calculate date distance to column of type "
													+ startDateColumn.getType());
		}
		// we can now safely cast to Field of type Date
		Field<Date> startDate = DSL.field(startDateColumn.getName(), Date.class);
		return DSL.dateDiff(timeUnit, startDate, endDate).as(select.getLabel());
	}

	private Date getEndDate(ConversionContext context) {
		LocalDate endDate;
		// if a date restriction is set, the max of the date restriction equals the end date of the date distance
		if (Objects.nonNull(context.getDateRestricionRange())) {
			endDate = context.getDateRestricionRange().getMax();
		}
		else {
			// otherwise the current date is the upper bound
			endDate = dateNowSupplier.get();
		}
		return Date.valueOf(endDate);
	}

	@Override
	public Class<DateDistanceSelect> getConversionClass() {
		return DateDistanceSelect.class;
	}
}
