package com.bakdata.conquery.sql.translate.query;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.JoinMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SchemaSql;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SharedAliases;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.translate.query.aggregation.DateAggregationDates;
import com.bakdata.conquery.sql.translate.query.aggregation.DateAggregationTranslator;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

/** Composes query-step IR, including negation and date aggregation, from resolved translate inputs. */
@UtilityClass
public final class QueryStepComposer {

	private static final String NEGATED_CTE_SUFFIX = "_negated";

	/** Implements an anti-join against the entity schema used to enumerate all known entities. */
	public static QueryStep antiJoinWithAllEntities(
			QueryStep queryStep,
			EntitySchema entitySchema,
			DateAggregationAction dateAggregationAction,
			TranslatorDialect translatorDialect
	) {
		Field<String> queryStepPrimaryColumn = queryStep.getQualifiedSelects().getIds().getPrimaryColumn();
		Field<String> allIdsPrimaryColumn = SchemaSql.field(entitySchema.primaryId(), String.class);

		Table<?> joinedTable = table(SchemaSql.tableName(entitySchema.table()))
				.leftOuterJoin(table(name(queryStep.getCteName())))
				.on(allIdsPrimaryColumn.eq(queryStepPrimaryColumn));

		String cteName = queryStep.getCteName() + NEGATED_CTE_SUFFIX;
		Optional<ColumnDateRange> validityDate = switch (dateAggregationAction) {
			case BLOCK, MERGE, INTERSECT -> Optional.of(translatorDialect.emptyDateRange());
			case NEGATE -> Optional.of(translatorDialect.unboundedDateRange());
		};

		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(allIdsPrimaryColumn))
				.validityDate(validityDate.map(dateRange -> dateRange.asValidityDateRange(cteName)))
				.build();

		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(joinedTable)
				.conditions(List.of(queryStepPrimaryColumn.isNull()))
				.predecessor(queryStep)
				.build();
	}

	public static QueryStep joinSteps(
			List<QueryStep> queriesToJoin,
			JoinMode logicalOperation,
			DateAggregationAction dateAggregationAction,
			EntitySchema entitySchema,
			TranslatorDialect translatorDialect,
			SqlNameGenerator nameGenerator
	) {
		if (queriesToJoin.isEmpty()) {
			throw new IllegalArgumentException("Need at least one query step to compose");
		}
		if (queriesToJoin.stream().anyMatch(QueryStep::isNegate)) {
			return joinStepsContainingNegation(
					queriesToJoin,
					logicalOperation,
					dateAggregationAction,
					entitySchema,
					translatorDialect,
					nameGenerator
			);
		}

		return doJoin(queriesToJoin, logicalOperation, dateAggregationAction, translatorDialect, nameGenerator);
	}

	private static QueryStep doJoin(
			List<QueryStep> queriesToJoin,
			JoinMode logicalOperation,
			DateAggregationAction dateAggregationAction,
			TranslatorDialect translatorDialect,
			SqlNameGenerator nameGenerator
	) {
		if (queriesToJoin.size() == 1) {
			return queriesToJoin.getFirst();
		}

		String joinedNodeName = nameGenerator.joinedNodeName(logicalOperation);
		SqlIdColumns ids = QueryStepJoiner.coalesceIds(queriesToJoin);
		List<SqlSelect> mergedSelects = QueryStepJoiner.mergeSelects(queriesToJoin);
		TableLike<Record> joinedTable = QueryStepJoiner.join(queriesToJoin, logicalOperation);

		QueryStep.QueryStepBuilder joinedStepBuilder = QueryStep.builder()
				.cteName(joinedNodeName)
				.fromTable(joinedTable)
				.predecessors(queriesToJoin);

		DateAggregationDates dateAggregationDates = DateAggregationDates.forSteps(queriesToJoin);
		if (dateAggregationAction == DateAggregationAction.BLOCK || dateAggregationDates.dateAggregationImpossible()) {
			Optional<ColumnDateRange> stratificationDate = coalesceStratificationDates(queriesToJoin);
			return buildJoinedStep(ids, mergedSelects, Optional.empty(), stratificationDate, joinedStepBuilder);
		}
		if (dateAggregationDates.getValidityDates().size() == 1) {
			ColumnDateRange validityDate = dateAggregationDates.getValidityDates().getFirst();
			return buildJoinedStep(ids, mergedSelects, Optional.of(validityDate), Optional.empty(), joinedStepBuilder);
		}
		return buildStepAndAggregateDates(
				ids,
				mergedSelects,
				joinedStepBuilder,
				dateAggregationDates,
				dateAggregationAction,
				translatorDialect,
				nameGenerator
		);
	}

	/**
	 * Uses negated steps in an anti-join against their positive siblings, or against all entities when no positive
	 * sibling exists.
	 */
	private static QueryStep joinStepsContainingNegation(
			List<QueryStep> queriesToJoin,
			JoinMode logicalOperation,
			DateAggregationAction dateAggregationAction,
			EntitySchema entitySchema,
			TranslatorDialect translatorDialect,
			SqlNameGenerator nameGenerator
	) {
		Map<Boolean, List<QueryStep>> byNegation = queriesToJoin.stream()
				.collect(Collectors.groupingBy(QueryStep::isNegate));

		List<QueryStep> withoutNegation = byNegation.get(false);
		List<QueryStep> withNegation = byNegation.get(true);
		if (withoutNegation == null) {
			QueryStep negateJoined = doJoin(
					withNegation,
					logicalOperation,
					dateAggregationAction,
					translatorDialect,
					nameGenerator
			);
			return antiJoinWithAllEntities(negateJoined, entitySchema, dateAggregationAction, translatorDialect);
		}

		String cteName = nameGenerator.joinedNodeName(logicalOperation) + NEGATED_CTE_SUFFIX;
		QueryStep nonNegateJoined = doJoin(
				withoutNegation,
				logicalOperation,
				dateAggregationAction,
				translatorDialect,
				nameGenerator
		);
		QueryStep negateJoined = doJoin(
				withNegation,
				logicalOperation,
				dateAggregationAction,
				translatorDialect,
				nameGenerator
		);

		Selects.SelectsBuilder selects = nonNegateJoined.getQualifiedSelects()
				.toBuilder()
				.sqlSelects(QueryStepJoiner.mergeSelects(List.of(nonNegateJoined, negateJoined)));

		if (logicalOperation == JoinMode.INNER) {
			Table<?> joinedTable = table(name(nonNegateJoined.getCteName()))
					.leftOuterJoin(table(name(negateJoined.getCteName())))
					.on(nonNegateJoined.getQualifiedSelects().getIds().getPrimaryColumn()
							.eq(negateJoined.getQualifiedSelects().getIds().getPrimaryColumn()));

			return QueryStep.builder()
					.cteName(cteName)
					.selects(selects.build())
					.fromTable(joinedTable)
					.conditions(List.of(negateJoined.getQualifiedSelects().getIds().getPrimaryColumn().isNull()))
					.predecessors(List.of(nonNegateJoined, negateJoined))
					.build();
		}

		negateJoined = DateAggregationTranslator.invert(negateJoined, translatorDialect, nameGenerator);
		Field<String> allIdsPrimaryColumn = SchemaSql.field(entitySchema.primaryId(), String.class);
		Field<String> negatePrimaryColumn = negateJoined.getQualifiedSelects().getIds().getPrimaryColumn();
		Field<String> nonNegatePrimaryColumn = nonNegateJoined.getQualifiedSelects().getIds().getPrimaryColumn();

		Condition infinityRangeCondition = negatePrimaryColumn.isNull().or(nonNegatePrimaryColumn.isNull());
		DateAggregationDates aggregationDates = DateAggregationDates.forValidityDates(List.of(
				nonNegateJoined.getQualifiedSelects().getValidityDate(),
				negateJoined.getQualifiedSelects().getValidityDate(),
				Optional.of(translatorDialect.conditionalUnboundedDateRange(infinityRangeCondition))
		));
		ColumnDateRange merged = DateAggregationTranslator.getAggregatedValidityDate(aggregationDates)
				.as(cteName + SharedAliases.DATES_COLUMN.getAlias());

		Field<String> coalescedId = DSL.coalesce(nonNegatePrimaryColumn, allIdsPrimaryColumn)
				.as(SharedAliases.PRIMARY_COLUMN.getAlias());
		selects = selects
				.ids(new SqlIdColumns(coalescedId))
				.validityDate(Optional.of(merged));

		Table<?> joinedTable = table(SchemaSql.tableName(entitySchema.table()))
				.leftOuterJoin(table(name(negateJoined.getCteName())))
				.on(allIdsPrimaryColumn.eq(negatePrimaryColumn))
				.leftOuterJoin(table(name(nonNegateJoined.getCteName())))
				.on(allIdsPrimaryColumn.eq(nonNegatePrimaryColumn));

		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects.build())
				.fromTable(joinedTable)
				.conditions(List.of(negatePrimaryColumn.isNull().or(nonNegatePrimaryColumn.isNotNull())))
				.predecessors(List.of(nonNegateJoined, negateJoined))
				.build();
	}

	private static QueryStep buildJoinedStep(
			SqlIdColumns ids,
			List<SqlSelect> mergedSelects,
			Optional<ColumnDateRange> validityDate,
			Optional<ColumnDateRange> stratificationDate,
			QueryStep.QueryStepBuilder builder
	) {
		Selects selects = Selects.builder()
				.ids(ids)
				.stratificationDate(stratificationDate)
				.validityDate(validityDate)
				.sqlSelects(mergedSelects)
				.build();
		return builder.selects(selects).build();
	}

	private static QueryStep buildStepAndAggregateDates(
			SqlIdColumns ids,
			List<SqlSelect> mergedSelects,
			QueryStep.QueryStepBuilder builder,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction,
			TranslatorDialect translatorDialect,
			SqlNameGenerator nameGenerator
	) {
		List<SqlSelect> withAllValidityDates = new ArrayList<>(mergedSelects);
		withAllValidityDates.addAll(dateAggregationDates.allStartsAndEnds());
		QueryStep joinedStep = buildJoinedStep(ids, withAllValidityDates, Optional.empty(), Optional.empty(), builder);

		return DateAggregationTranslator.aggregate(
				joinedStep,
				mergedSelects,
				dateAggregationDates,
				dateAggregationAction,
				translatorDialect,
				nameGenerator
		);
	}

	private static Optional<ColumnDateRange> coalesceStratificationDates(List<QueryStep> queriesToJoin) {
		return queriesToJoin.stream()
				.map(QueryStep::getQualifiedSelects)
				.map(Selects::getStratificationDate)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.reduce(ColumnDateRange::coalesce);
	}

}
