package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import static org.jooq.impl.DSL.field;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DateDistanceAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.aggregator.DateDistanceSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.jooq.Field;
import org.jooq.impl.DSL;

@CPSType(id = "DATE_DISTANCE", base = Select.class)
@Getter
@Setter
public class DateDistanceSelect extends SingleColumnSelect {

	@NotNull
	private ChronoUnit timeUnit = ChronoUnit.YEARS;

	@JsonCreator
	public DateDistanceSelect(ColumnId column) {
		super(column);
	}

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE, MajorTypeId.DATE_RANGE);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new DateDistanceAggregator(getColumn().resolve(), getTimeUnit());
	}

	@Override
	public SelectConverter<DateDistanceSelect> createConverter() {
		return new DateDistanceSqlAggregator();
	}

	@Override
	public ResultType getResultType() {
		return ResultType.Primitive.INTEGER;
	}

	@Override
	public Field<?> convert(String table, SqlFunctionProvider provider, ConversionContext context) {
		Column column = getColumn().resolve();

		Field<Date> startDate;
		if (column.getType() == MajorTypeId.DATE) {
			startDate = field(DSL.name(table, column.getName()), Date.class);
		}
		else if (column.getType() == MajorTypeId.DATE_RANGE) {
			StratificationFunctions stratificationFunctions = StratificationFunctions.create(context);
			Field<Date> daterangeColumn = field(DSL.name(table, column.getName()), Date.class);
			startDate = stratificationFunctions.lower(ColumnDateRange.of(daterangeColumn));
		}
		else {
			startDate = DSL.noField(Date.class);
		}

		Field<Date> endDate = getEndDate(context);

		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		return functionProvider.dateDistance(timeUnit, startDate, endDate);
	}

	private Field<Date> getEndDate(ConversionContext conversionContext) {

		SqlFunctionProvider functionProvider = conversionContext.getSqlDialect().getFunctionProvider();

		// if there is a stratification active, the upper bound of the stratification date is the end date
		if (conversionContext.isWithStratification()) {
			ColumnDateRange stratificationDate = conversionContext.getStratificationTable().getQualifiedSelects().getStratificationDate().get();
			ColumnDateRange dualColumn = functionProvider.toDualColumn(stratificationDate);
			// end date is always treated exclusive, so we get the actual end date when subtracting 1 day
			return functionProvider.addDays(dualColumn.getEnd(), DSL.val(-1));
		}

		LocalDate endDate;
		// if a date restriction is set, the max of the date restriction equals the end date of the date distance
		// but there is also the possibility that the user set's an empty daterange which will be non-null but with null values
		CDateRange dateRestriction = conversionContext.getDateRestrictionRange();
		if (dateRestriction != null && dateRestriction.getMax() != null) {
			endDate = dateRestriction.getMax();
		}
		else {
			// otherwise the current date is the upper bound
			endDate = conversionContext.getSqlDialect().getDateNowSupplier().getLocalDateNow();
		}
		return functionProvider.toDateField(Date.valueOf(endDate).toString());
	}
}
