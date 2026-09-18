package com.bakdata.conquery.sql.conversion.dialect;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
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
	default Field<Date> minimumDate() {
		return getFunctionProvider().getMinDateExpression();
	}

	@Override
	default Field<Date> maximumDate() {
		return getFunctionProvider().getMaxDateExpression();
	}

	@Override
	default <T> Field<T> anyValue(Field<T> field) {
		return getFunctionProvider().anyValue(field);
	}

	@Override
	default Field<?> renderDateRange(Field<Date> start, Field<Date> end) {
		return getFunctionProvider().dateRangeToField(ColumnDateRange.of(start, end));
	}

	@Override
	default Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end) {
		return getFunctionProvider().dateRangeAggregation(ColumnDateRange.of(start, end));
	}

	@Override
	default Field<String> externalId(String id) {
		return getFunctionProvider().externalId(id);
	}

	@Override
	default Field<?> externalStringValues(List<String> values) {
		return getFunctionProvider().asArrayRepr(values);
	}

	@Override
	default Table<? extends Record> literalSelectTable() {
		return getFunctionProvider().getNoOpTable();
	}

	@Override
	default ColumnDateRange dateRangeLiteral(DateRange dateRange) {
		CDateRange legacyDateRange = CDateRange.of(
				dateRange.startInclusive().orElse(null),
				dateRange.endInclusive().orElse(null)
		);
		return getFunctionProvider().forCDateRange(legacyDateRange);
	}

	@Override
	default QueryStep unnestDateRange(ColumnDateRange dateRange, QueryStep predecessor, String cteName) {
		return getFunctionProvider().unnestDaterange(dateRange, predecessor, cteName);
	}

	@Override
	default String regexAnyCharacters() {
		return getFunctionProvider().getAnyCharRegex();
	}

	@Override
	default Condition regexMatches(Field<String> field, String pattern) {
		return getFunctionProvider().likeRegex(field, pattern);
	}

	@Override
	default Field<Integer> dateDistance(ChronoUnit unit, Field<Date> startDate, LocalDate endDate) {
		return getFunctionProvider().dateDistance(
				unit,
				startDate,
				getFunctionProvider().toDateField(endDate.toString())
		);
	}

	StratificationFunctions getStratificationFunctions();

	SqlFunctionProvider getFunctionProvider();

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
