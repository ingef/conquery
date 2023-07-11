package com.bakdata.conquery.sql.conversion.select;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Objects;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import org.jooq.Field;

public class DateDistanceConverter implements SelectConverter<DateDistanceSelect> {

	private final DateNowSupplier dateNowSupplier;

	public DateDistanceConverter(DateNowSupplier dateNowSupplier) {
		this.dateNowSupplier = dateNowSupplier;
	}

	@Override
	public Field<Integer> convert(DateDistanceSelect select, ConversionContext context) {

		Column startDateColumn = select.getColumn();
		if (startDateColumn.getType() != MajorTypeId.DATE) {
			throw new UnsupportedOperationException("Can't calculate date distance to column of type "
													+ startDateColumn.getType());
		}

		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunction();
		Date endDate = getEndDate(context);

		return functionProvider.dateDistance(select.getTimeUnit(), startDateColumn, endDate)
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
