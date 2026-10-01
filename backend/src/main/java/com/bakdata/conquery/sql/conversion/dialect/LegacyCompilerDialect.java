package com.bakdata.conquery.sql.conversion.dialect;

import org.jooq.DataType;
import org.jooq.SortField;
import org.jooq.OrderField;
import java.util.function.Function;
import java.util.Collection;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.conversion.operation.SelectConversionContext;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.rendering.QueryStepRenderer;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQAndConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQDateRestrictionConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQExternalConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQNegationConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQOrConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQYesConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CQConceptConverter;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.query.AbsoluteFormQueryConverter;
import com.bakdata.conquery.sql.conversion.query.CQReusedQueryConverter;
import com.bakdata.conquery.sql.conversion.query.ConceptQueryConverter;
import com.bakdata.conquery.sql.conversion.query.EntityDateQueryConverter;
import com.bakdata.conquery.sql.conversion.query.FormConversionHelper;
import com.bakdata.conquery.sql.conversion.query.RelativFormQueryConverter;
import com.bakdata.conquery.sql.conversion.query.SecondaryIdQueryConverter;
import com.bakdata.conquery.sql.conversion.query.TableExportQueryConverter;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

/**
 * Temporary backend adapter exposing services required by the legacy SQL compiler.
 *
 * <p>The framework-neutral dialect contract lives in {@link CompilerDialect}. This adapter retains dependencies on
 * backend query DTOs, converter registries, and legacy compiler services until those implementations move into the SQL
 * connector. New connector code must not depend on this interface.</p>
 */
public interface LegacyCompilerDialect extends CompilerDialect {

	@Override
	default Condition unconditionalJoinCondition() {
		return getCompilerDialect().unconditionalJoinCondition();
	}

	@Override
	default ConnectorSqlSelects distinctSelect(BuiltInSelects.Values select, SelectConversionContext context) {
		return getCompilerDialect().distinctSelect(select, context);
	}

	@Override
	default ColumnDateRange toDualColumn(ColumnDateRange range) {
		return getCompilerDialect().toDualColumn(range);
	}

	@Override
	default Condition orAgg(Field<Boolean> field) {
		return getCompilerDialect().orAgg(field);
	}

	@Override
	default Field<?> arrayOut(List<Field<String>> fields) {
		return getCompilerDialect().arrayOut(fields);
	}

	@Override
	default <T> Field<T> cast(Field<?> field, DataType<T> type) {
		return getCompilerDialect().cast(field, type);
	}

	@Override
	default Field<String> stringAggregation(Field<String> field, Field<String> delimiter, List<Field<?>> orderByFields) {
		return getCompilerDialect().stringAggregation(field, delimiter, orderByFields);
	}

	@Override
	default Collection<? extends OrderField<?>> orderByValidityDates(
			Function<Field<?>, ? extends SortField<?>> ordering, List<Field<?>> validityDateFields) {
		return getCompilerDialect().orderByValidityDates(ordering, validityDateFields);
	}

	@Override
	default <T> Field<T> random(Field<T> field) {
		return getCompilerDialect().random(field);
	}

	@Override
	default Field<Date> minimumDate() {
		return getCompilerDialect().minimumDate();
	}

	@Override
	default Field<Date> maximumDate() {
		return getCompilerDialect().maximumDate();
	}

	@Override
	default <T> Field<T> anyValue(Field<T> field) {
		return getCompilerDialect().anyValue(field);
	}

	@Override
	default Field<?> renderDateRange(Field<Date> start, Field<Date> end) {
		return getCompilerDialect().renderDateRange(start, end);
	}

	@Override
	default Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end) {
		return getCompilerDialect().aggregateDateRanges(start, end);
	}

	@Override
	default Field<String> externalId(String id) {
		return getCompilerDialect().externalId(id);
	}

	@Override
	default Field<?> externalStringValues(List<String> values) {
		return getCompilerDialect().externalStringValues(values);
	}

	@Override
	default Table<? extends Record> literalSelectTable() {
		return getCompilerDialect().literalSelectTable();
	}

	@Override
	default ColumnDateRange dateRangeLiteral(DateRange dateRange) {
		return getCompilerDialect().dateRangeLiteral(dateRange);
	}

	@Override
	default QueryStep unnestDateRange(ColumnDateRange dateRange, QueryStep predecessor, String cteName) {
		return getCompilerDialect().unnestDateRange(dateRange, predecessor, cteName);
	}

	@Override
	default String regexAnyCharacters() {
		return getCompilerDialect().regexAnyCharacters();
	}

	@Override
	default Condition regexMatches(Field<String> field, String pattern) {
		return getCompilerDialect().regexMatches(field, pattern);
	}

	@Override
	default Field<Integer> dateDistance(ChronoUnit unit, Field<Date> startDate, LocalDate endDate) {
		return getCompilerDialect().dateDistance(unit, startDate, endDate);
	}

	StratificationFunctions getStratificationFunctions();

	@Override
	default Field<Integer> dateDistance(ChronoUnit unit, Field<Date> startDate, Field<Date> endDate) {
		return getCompilerDialect().dateDistance(unit, startDate, endDate);
	}

	@Override
	default Field<Date> addDays(Field<Date> date, Field<Integer> days) {
		return getCompilerDialect().addDays(date, days);
	}

	@Override
	default ColumnDateRange dateRange(Field<Date> start, Field<Date> inclusiveEnd) {
		return getCompilerDialect().dateRange(start, inclusiveEnd);
	}

	@Override
	default ColumnDateRange dateRangeColumn(Field<?> range) {
		// Both existing SQL providers use a single physical column as inclusive start and end.
		Field<Date> date = range.coerce(Date.class);
		return dateRange(date, date);
	}

	@Override
	default Field<String> yearQuarter(Field<Date> date) {
		return getCompilerDialect().yearQuarter(date);
	}

	@Override
	default Field<Date> quarterStart(Field<Date> date) {
		return getCompilerDialect().quarterStart(date);
	}

	@Override
	default Field<Date> nextQuarterStart(Field<Date> date) {
		return getCompilerDialect().nextQuarterStart(date);
	}

	SqlFunctionProvider getFunctionProvider();

	CompilerDialect getCompilerDialect();

	List<NodeConverter<? extends Visitable>> getNodeConverters(DSLContext context);

	default List<NodeConverter<? extends Visitable>> getDefaultNodeConverters(DSLContext dslContext) {

		QueryStepRenderer queryStepRenderer = new QueryStepRenderer(dslContext);
		FormConversionHelper formConversionUtil = new FormConversionHelper(queryStepRenderer);

		return List.of(
				new CQDateRestrictionConverter(),
				new CQAndConverter(),
				new CQOrConverter(),
				new CQNegationConverter(),
				new CQYesConverter(),
				new CQConceptConverter(),
				new CQExternalConverter(),
				new CQReusedQueryConverter(),
				new ConceptQueryConverter(queryStepRenderer),
				new SecondaryIdQueryConverter(),
				new AbsoluteFormQueryConverter(formConversionUtil),
				new EntityDateQueryConverter(formConversionUtil),
				new RelativFormQueryConverter(formConversionUtil),
				new TableExportQueryConverter(queryStepRenderer)
		);
	}

	default Map<Class<? extends Select>, ? extends SelectConverter<? extends Select>> getSelectConverterOverrides() {
		return Collections.emptyMap();
	}

	default SelectConverter<Select> getSelectConverter(Select select) {
		SelectConverter<Select> maybeOverride = (SelectConverter<Select>) getSelectConverterOverrides().get(select.getClass());

		if (maybeOverride != null) {
			return maybeOverride;
		}

		return select.createConverter();
	}
}
