package com.bakdata.conquery.sql.translate.conversion.aggregation;

import static com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep.PREPROCESSING;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.translate.conversion.select.SelectConversionContext;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep;
import com.bakdata.conquery.sql.translate.query.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingSelectTranslator;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingSelectPreparation;
import com.bakdata.conquery.sql.model.schema.DateColumns;

public class DaterangeSelectUtil {
	public static ConnectorSqlSelects createForSelect(
			DateColumns dates,
			AggregationFunction aggregationFunction,
			SelectConversionContext context
	) {
		String alias = context.alias();
		TranslatorDialect functionProvider = context.dialect();

		ColumnDateRange daterange = DateAggregationConverters.dateRange(dates, new AggregationConversionContext(context.dialect(), context.nameGenerator(), context.tables(), context.ids(), alias)).as(alias);
		List<SqlSelect> rootSelects = daterange.toFields().stream()
											   .map(FieldWrapper::new)
											   .collect(Collectors.toList());

		IntervalPackingSelectPreparation preparation = prepareIntervalSelect(
				alias,
				daterange,
				context.ids(),
				context.tables(),
				context
		);
		ColumnDateRange qualified = preparation.dateRange();
		FieldWrapper<?> aggregationField = aggregationFunction.apply(qualified, alias, functionProvider);

		QueryStep intervalPackingSelectsStep = IntervalPackingSelectTranslator.translateArbitrarySelect(
				preparation.predecessor(),
				qualified,
				aggregationField,
				preparation.tables(),
				context.dialect()
		);

		SqlTables tables = context.tables();
		ExtractingSqlSelect<?> finalSelect = aggregationField.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(rootSelects)
								  .additionalPredecessor(Optional.of(intervalPackingSelectsStep))
								  .finalSelect(finalSelect)
								  .build();
	}

	private static IntervalPackingSelectPreparation prepareIntervalSelect(
			String alias,
			ColumnDateRange daterange,
			SqlIdColumns ids,
			SqlTables tables,
			SelectConversionContext context
	) {
		return IntervalPackingSelectTranslator.prepareArbitrarySelect(
				alias,
				tables.cteName(PREPROCESSING),
				ids,
				daterange,
				context.dialect(),
				context.nameGenerator()
		);
	}

	@FunctionalInterface
	public interface AggregationFunction {
		FieldWrapper<?> apply(ColumnDateRange daterange, String alias, TranslatorDialect functionProvider);
	}

}
