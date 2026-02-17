package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import static org.jooq.impl.DSL.field;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.event.DateDistanceFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.aggregator.DateDistanceSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter
@Setter
@Slf4j
@CPSType(id = "DATE_DISTANCE", base = Filter.class)
public class DateDistanceFilter extends SingleColumnFilter<Range.LongRange> {

	@NotNull
	private ChronoUnit timeUnit = ChronoUnit.YEARS;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		MajorTypeId type = getColumn().resolve().getType();
		if (type != MajorTypeId.DATE) {
			throw new ConceptConfigurationException(getConnector(), "DATE_DISTANCE filter is incompatible with columns of type " + type);
		}

		f.setType(FrontendFilterType.Fields.INTEGER_RANGE);
	}

	@Override
	public FilterNode<?> createFilterNode(Range.LongRange value) {
		return new DateDistanceFilterNode(getColumn().resolve(), timeUnit, value);
	}

	@Override
	public FilterConverter<DateDistanceFilter, Range.LongRange> createConverter() {
		return new DateDistanceSqlAggregator();
	}

	@Override
	public Condition convertEventFilter(String table, Range.LongRange longRange, ConversionContext conversionContext) {
		Column column = getColumn().resolve();

		Field<Date> startDate;
		if (column.getType() == MajorTypeId.DATE) {
			startDate = field(DSL.name(table, column.getName()), Date.class);
		}
		else if(column.getType() == MajorTypeId.DATE_RANGE){
			StratificationFunctions stratificationFunctions = StratificationFunctions.create(conversionContext);
			Field<Date> daterangeColumn = field(DSL.name(table, column.getName()), Date.class);
			startDate = stratificationFunctions.lower(ColumnDateRange.of(daterangeColumn));
		}
		else {
			startDate = DSL.noField(Date.class);
		}

		Field<Date> endDate = getEndDate(conversionContext);

		SqlFunctionProvider functionProvider = conversionContext.getSqlDialect().getFunctionProvider();
		return functionProvider.dateDistance(timeUnit, startDate, endDate).between(longRange.getMin().intValue(), longRange.getMax().intValue());
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
