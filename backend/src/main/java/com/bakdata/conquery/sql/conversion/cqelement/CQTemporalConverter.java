package com.bakdata.conquery.sql.conversion.cqelement;

import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.specific.CQTemporal;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalRelationMode;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSelector;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.ConqueryJoinType;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.google.common.base.Preconditions;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.TableLike;

/**
 * SQL conversion of the two-pass temporal evaluation implemented by {@link CQTemporal}.
 */
public class CQTemporalConverter implements NodeConverter<CQTemporal> {

	private static final String INDEX_PARTITION = "index_partition";
	private static final String COMPARE_PARTITION = "compare_partition";
	private static final String TOTAL_COUNT = "total_count";
	private static final String MATCHED_COUNT = "matched_count";

	@Override
	public Class<CQTemporal> getConversionClass() {
		return CQTemporal.class;
	}

	@Override
	public ConversionContext convert(CQTemporal temporal, ConversionContext context) {
		String prefix = context.getNameGenerator().temporalName();

		QueryStep index = convertChild(temporal.getIndex(), context.createChildContext());
		QueryStep rankedIndex = rankIndex(index, temporal.getIndexSelector(), prefix + "-rank-index");
		QueryStep indexWindows = selectIndexWindows(rankedIndex, temporal, prefix + "-index-windows", context);

		ConversionContext outerContext = context.createChildContext()
				.withStratificationTable(indexWindows)
				.withDisableAggregationFilters(true);
		QueryStep outerCompare = convertChild(temporal.getCompare(), outerContext);
		QueryStep rankedCandidates = rankCandidates(outerCompare, prefix + "-rank-candidates", context);
		QueryStep candidateWindows = createCandidateWindows(rankedCandidates, prefix + "-candidate-windows");

		ConversionContext innerContext = context.createChildContext()
				.withStratificationTable(candidateWindows)
				.withDisableAggregationFilters(false);
		QueryStep innerCompare = convertChild(temporal.getCompare(), innerContext);
		QueryStep successfulCandidates = collapseSuccessfulCandidates(innerCompare, prefix + "-successful-candidates", context);
		QueryStep matchedIndex = matchedIndexPeriods(successfulCandidates, prefix + "-matched-index");

		QueryStep eligible = null;
		if (temporal.getIndexSelector() == TemporalSelector.ALL) {
			QueryStep totalCount = countPartitions(indexWindows, 1, TOTAL_COUNT, prefix + "-total-count");
			QueryStep matchedCount = countPartitions(matchedIndex, 1, MATCHED_COUNT, prefix + "-matched-count");
			eligible = eligibleEntities(totalCount, matchedCount, prefix + "-eligible", context);
			matchedIndex = retainEligible(matchedIndex, eligible, prefix + "-eligible-index", context);
			successfulCandidates = retainEligible(successfulCandidates, eligible, prefix + "-eligible-candidates", context);
		}

		QueryStep indexResult = createIndexResult(indexWindows, matchedIndex, prefix + "-index-result", context);
		QueryStep compareResult = aggregateCompareResults(successfulCandidates, prefix + "-compare-result", context);
		QueryStep temporalResult = joinResults(indexResult, compareResult, prefix + "-result", context);
		return context.withQueryStep(temporalResult);
	}

	private static QueryStep convertChild(com.bakdata.conquery.apiv1.query.CQElement child, ConversionContext childContext) {
		ConversionContext converted = childContext.getNodeConversions().convert(child, childContext);
		QueryStep convertedStep = converted.getLastConvertedStep();
		if (!convertedStep.isNegate()) {
			return convertedStep;
		}
		return QueryStepJoiner.antiJoinWithUniverse(convertedStep, childContext, DateAggregationAction.BLOCK);
	}

	private static QueryStep rankIndex(QueryStep index, TemporalSelector selector, String cteName) {
		Selects source = index.getQualifiedSelects();
		ColumnDateRange validity = requireValidityDate(source, "Temporal index");
		List<SortField<?>> ordering = selector == TemporalSelector.LATEST
				? List.of(validity.getStart().desc(), validity.getEnd().desc())
				: List.of(validity.getStart().asc(), validity.getEnd().asc());
		Field<Integer> partition = rowNumber()
				.over(partitionBy(source.getIds().toFields()).orderBy(ordering))
				.as(INDEX_PARTITION);

		Selects selects = source.toBuilder().sqlSelect(new FieldWrapper<>(partition)).build();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(QueryStep.toTableLike(index.getCteName()))
				.predecessor(index)
				.build();
	}

	private static QueryStep selectIndexWindows(
			QueryStep ranked,
			CQTemporal temporal,
			String cteName,
			ConversionContext context
	) {
		Selects source = ranked.getQualifiedSelects();
		ColumnDateRange validity = requireValidityDate(source, "Temporal index");
		Field<Integer> partition = field(name(ranked.getCteName(), INDEX_PARTITION), Integer.class);
		SqlIdColumns ids = source.getIds().withTemporalPartition(partition);
		ColumnDateRange temporalBounds = temporalBounds(validity, temporal.getMode(), temporal.getIndexSelector(), context.getFunctionProvider())
				.as("temporal_bounds");

		List<Condition> conditions = new ArrayList<>();
		if (temporal.getIndexSelector() == TemporalSelector.EARLIEST || temporal.getIndexSelector() == TemporalSelector.LATEST) {
			conditions.add(partition.eq(inline(1)));
		}
		anchorCondition(validity, temporal.getMode(), temporal.getIndexSelector(), context.getFunctionProvider()).ifPresent(conditions::add);

		Selects selects = Selects.builder()
				.ids(ids)
				.validityDate(Optional.of(validity))
				.stratificationDate(Optional.of(temporalBounds))
				.sqlSelects(withoutField(source.getSqlSelects(), INDEX_PARTITION))
				.build();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(QueryStep.toTableLike(ranked.getCteName()))
				.conditions(conditions)
				.predecessor(ranked)
				.build();
	}

	private static QueryStep rankCandidates(QueryStep outerCompare, String cteName, ConversionContext context) {
		Selects source = outerCompare.getQualifiedSelects();
		ColumnDateRange compareDate = requireValidityDate(source, "Temporal compare");
		ColumnDateRange bounds = source.getStratificationDate().orElseThrow(() -> new IllegalStateException("Temporal compare requires its index window"));
		ColumnDateRange clipped = context.getFunctionProvider().intersection(compareDate, bounds).as("candidate_date");
		Field<Integer> partition = rowNumber()
				.over(partitionBy(source.getIds().toFields()).orderBy(clipped.getStart(), clipped.getEnd()))
				.as(COMPARE_PARTITION);

		Selects selects = Selects.builder()
				.ids(source.getIds())
				.validityDate(Optional.of(clipped))
				.sqlSelect(new FieldWrapper<>(partition))
				.build();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(QueryStep.toTableLike(outerCompare.getCteName()))
				.conditions(List.of(clipped.getStart().lessThan(clipped.getEnd())))
				.predecessor(outerCompare)
				.build();
	}

	private static QueryStep createCandidateWindows(QueryStep rankedCandidates, String cteName) {
		Selects source = rankedCandidates.getQualifiedSelects();
		Field<Integer> partition = field(name(rankedCandidates.getCteName(), COMPARE_PARTITION), Integer.class);
		SqlIdColumns ids = source.getIds().withTemporalPartition(partition);
		ColumnDateRange candidate = requireValidityDate(source, "Temporal candidate");

		Selects selects = Selects.builder()
				.ids(ids)
				.stratificationDate(Optional.of(candidate))
				.build();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(QueryStep.toTableLike(rankedCandidates.getCteName()))
				.predecessor(rankedCandidates)
				.build();
	}

	private static QueryStep collapseSuccessfulCandidates(QueryStep innerCompare, String cteName, ConversionContext context) {
		Selects source = innerCompare.getQualifiedSelects();
		ColumnDateRange compareDate = requireValidityDate(source, "Temporal inner compare");
		ColumnDateRange candidate = source.getStratificationDate().orElseThrow(() -> new IllegalStateException("Temporal inner compare requires its candidate window"));
		ColumnDateRange clipped = context.getFunctionProvider().intersection(compareDate, candidate);
		List<SqlSelect> aggregated = anyValue(source.getSqlSelects(), context.getFunctionProvider());

		Selects selects = Selects.builder()
				.ids(source.getIds())
				.sqlSelects(aggregated)
				.build();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(QueryStep.toTableLike(innerCompare.getCteName()))
				.conditions(List.of(clipped.getStart().lessThan(clipped.getEnd())))
				.groupBy(source.getIds().toFields())
				.predecessor(innerCompare)
				.build();
	}

	private static QueryStep matchedIndexPeriods(QueryStep successfulCandidates, String cteName) {
		SqlIdColumns ids = successfulCandidates.getQualifiedSelects().getIds().withoutTemporalPartition();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder().ids(ids).build())
				.fromTable(QueryStep.toTableLike(successfulCandidates.getCteName()))
				.groupBy(ids.toFields())
				.predecessor(successfulCandidates)
				.build();
	}

	private static QueryStep countPartitions(QueryStep sourceStep, int partitionsToRemove, String alias, String cteName) {
		SqlIdColumns ids = sourceStep.getQualifiedSelects().getIds();
		for (int index = 0; index < partitionsToRemove; index++) {
			ids = ids.withoutTemporalPartition();
		}
		Field<Integer> count = count().cast(Integer.class).as(alias);
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder().ids(ids).sqlSelect(new FieldWrapper<>(count)).build())
				.fromTable(QueryStep.toTableLike(sourceStep.getCteName()))
				.groupBy(ids.toFields())
				.predecessor(sourceStep)
				.build();
	}

	private static QueryStep eligibleEntities(QueryStep total, QueryStep matched, String cteName, ConversionContext context) {
		Selects left = total.getQualifiedSelects();
		Selects right = matched.getQualifiedSelects();
		TableLike<Record> joined = context.getFunctionProvider().innerJoin(
				QueryStep.toTableLike(total.getCteName()).asTable(),
				QueryStep.toTableLike(matched.getCteName()).asTable(),
				left.getIds().join(right.getIds())
		);
		Field<Integer> totalCount = field(name(total.getCteName(), TOTAL_COUNT), Integer.class);
		Field<Integer> matchedCount = field(name(matched.getCteName(), MATCHED_COUNT), Integer.class);
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder().ids(left.getIds()).build())
				.fromTable(joined)
				.conditions(List.of(totalCount.eq(matchedCount)))
				.predecessors(List.of(total, matched))
				.build();
	}

	private static QueryStep retainEligible(QueryStep sourceStep, QueryStep eligible, String cteName, ConversionContext context) {
		Selects source = sourceStep.getQualifiedSelects();
		Selects eligibleSelects = eligible.getQualifiedSelects();
		TableLike<Record> joined = context.getFunctionProvider().innerJoin(
				QueryStep.toTableLike(sourceStep.getCteName()).asTable(),
				QueryStep.toTableLike(eligible.getCteName()).asTable(),
				source.getIds().join(eligibleSelects.getIds())
		);
		return QueryStep.builder()
				.cteName(cteName)
				.selects(source)
				.fromTable(joined)
				.predecessors(List.of(sourceStep, eligible))
				.build();
	}

	private static QueryStep createIndexResult(
			QueryStep indexWindows,
			QueryStep matchedIndex,
			String cteName,
			ConversionContext context
	) {
		Selects index = indexWindows.getQualifiedSelects();
		Selects matched = matchedIndex.getQualifiedSelects();
		TableLike<Record> joined = context.getFunctionProvider().innerJoin(
				QueryStep.toTableLike(indexWindows.getCteName()).asTable(),
				QueryStep.toTableLike(matchedIndex.getCteName()).asTable(),
				index.getIds().join(matched.getIds())
		);
		SqlIdColumns outputIds = index.getIds().withoutTemporalPartition();
		Selects selects = Selects.builder()
				.ids(outputIds)
				.validityDate(index.getValidityDate())
				.sqlSelects(index.getSqlSelects())
				.build();
		List<Field<?>> groupBy = Stream.concat(selects.nonExplicitSelects().stream(), selects.explicitSelects().stream()).toList();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(joined)
				.groupBy(groupBy)
				.predecessors(List.of(indexWindows, matchedIndex))
				.build();
	}

	private static QueryStep aggregateCompareResults(QueryStep successfulCandidates, String cteName, ConversionContext context) {
		Selects source = successfulCandidates.getQualifiedSelects();
		SqlIdColumns ids = source.getIds();
		Field<Integer> comparePartition = ids.getTemporalPartition().orElseThrow();
		ids = ids.withoutTemporalPartition();
		Field<Integer> indexPartition = ids.getTemporalPartition().orElseThrow();
		ids = ids.withoutTemporalPartition();

		List<Field<?>> ordering = List.of(indexPartition, comparePartition);
		List<SqlSelect> lists = source.getSqlSelects().stream()
				.flatMap(select -> select.toFields().stream())
				.map(value -> new FieldWrapper<>(context.getFunctionProvider().listAggregation(value, ordering).as(value.getName())))
				.map(SqlSelect.class::cast)
				.toList();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder().ids(ids).sqlSelects(lists).build())
				.fromTable(QueryStep.toTableLike(successfulCandidates.getCteName()))
				.groupBy(ids.toFields())
				.predecessor(successfulCandidates)
				.build();
	}

	private static QueryStep joinResults(QueryStep indexResult, QueryStep compareResult, String cteName, ConversionContext context) {
		Selects index = indexResult.getQualifiedSelects();
		Selects compare = compareResult.getQualifiedSelects();
		TableLike<Record> joined = QueryStepJoiner.constructJoinedTable(List.of(indexResult, compareResult), ConqueryJoinType.INNER_JOIN, context);
		Selects selects = Selects.builder()
				.ids(index.getIds())
				.validityDate(index.getValidityDate())
				.sqlSelects(Stream.concat(index.getSqlSelects().stream(), compare.getSqlSelects().stream()).toList())
				.build();
		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(joined)
				.predecessors(List.of(indexResult, compareResult))
				.build();
	}

	static ColumnDateRange temporalBounds(
			ColumnDateRange index,
			TemporalRelationMode mode,
			TemporalSelector selector,
			SqlFunctionProvider functionProvider
	) {
		if (mode instanceof TemporalRelationMode.While) {
			return ColumnDateRange.of(index.getStart(), index.getEnd());
		}
		if (mode instanceof TemporalRelationMode.After after) {
			Field<Date> anchor = selector == TemporalSelector.EARLIEST
					? index.getStart()
					: functionProvider.addDays(index.getEnd(), inline(-1));
			return afterBounds(anchor, after.getDays(), functionProvider);
		}
		if (mode instanceof TemporalRelationMode.Before before) {
			Field<Date> anchor = selector == TemporalSelector.LATEST
					? functionProvider.addDays(index.getEnd(), inline(-1))
					: index.getStart();
			return beforeBounds(anchor, before.getDays(), functionProvider);
		}
		throw new IllegalStateException("Unsupported temporal mode " + mode);
	}

	private static ColumnDateRange afterBounds(Field<Date> anchor, Range.IntegerRange days, SqlFunctionProvider functionProvider) {
		if (days == null || days.isAll()) {
			return ColumnDateRange.of(anchor, functionProvider.getMaxDateExpression());
		}
		Field<Date> start = days.hasLowerBound() ? functionProvider.addDays(anchor, inline(days.getMin())) : anchor;
		Field<Date> end = days.hasUpperBound()
				? functionProvider.addDays(anchor, inline(days.getMax() + 1))
				: functionProvider.getMaxDateExpression();
		return ColumnDateRange.of(start, end);
	}

	private static ColumnDateRange beforeBounds(Field<Date> anchor, Range.IntegerRange days, SqlFunctionProvider functionProvider) {
		if (days == null || days.isAll()) {
			return ColumnDateRange.of(functionProvider.getMinDateExpression(), functionProvider.addDays(anchor, inline(1)));
		}
		Field<Date> start = days.hasUpperBound()
				? functionProvider.addDays(anchor, inline(-days.getMax()))
				: functionProvider.getMinDateExpression();
		Field<Date> end = days.hasLowerBound()
				? functionProvider.addDays(anchor, inline(1 - days.getMin()))
				: functionProvider.addDays(anchor, inline(1));
		return ColumnDateRange.of(start, end);
	}

	private static Optional<Condition> anchorCondition(
			ColumnDateRange index,
			TemporalRelationMode mode,
			TemporalSelector selector,
			SqlFunctionProvider functionProvider
	) {
		if (mode instanceof TemporalRelationMode.While) {
			return Optional.empty();
		}
		if (mode instanceof TemporalRelationMode.After) {
			return Optional.of(selector == TemporalSelector.EARLIEST
					? index.getStart().ne(functionProvider.getMinDateExpression())
					: index.getEnd().ne(functionProvider.getMaxDateExpression()));
		}
		return Optional.of(selector == TemporalSelector.LATEST
				? index.getEnd().ne(functionProvider.getMaxDateExpression())
				: index.getStart().ne(functionProvider.getMinDateExpression()));
	}

	private static List<SqlSelect> withoutField(List<SqlSelect> selects, String fieldName) {
		return selects.stream().filter(select -> select.toFields().stream().noneMatch(field -> field.getName().equals(fieldName))).toList();
	}

	private static List<SqlSelect> anyValue(List<SqlSelect> selects, SqlFunctionProvider functionProvider) {
		return selects.stream()
				.flatMap(select -> select.toFields().stream())
				.map(value -> new FieldWrapper<>(functionProvider.anyValue(value).as(value.getName())))
				.map(SqlSelect.class::cast)
				.toList();
	}

	private static ColumnDateRange requireValidityDate(Selects selects, String description) {
		Preconditions.checkArgument(selects.getValidityDate().isPresent(), "%s must provide a validity date", description);
		return selects.getValidityDate().orElseThrow();
	}
}
