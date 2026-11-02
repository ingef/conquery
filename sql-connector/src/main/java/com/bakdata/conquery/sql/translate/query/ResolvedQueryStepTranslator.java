package com.bakdata.conquery.sql.translate.query;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.node.AndNode;
import com.bakdata.conquery.sql.model.node.ConceptNode;
import com.bakdata.conquery.sql.model.node.DateRestrictionNode;
import com.bakdata.conquery.sql.model.node.ExternalNode;
import com.bakdata.conquery.sql.model.node.NegationNode;
import com.bakdata.conquery.sql.model.node.OrNode;
import com.bakdata.conquery.sql.model.node.QueryNode;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.JoinMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;

/** Compiles a resolved query graph to reusable query-step IR without rendering a final projection. */
public final class ResolvedQueryStepTranslator {

	public TranslatedQuerySteps translate(
			ResolvedQuery query,
			TranslatorDialect dialect,
			SqlNameGenerator names,
			Optional<QueryStep> stratificationTable
	) {
		TranslatedQuerySteps compiled = translateNode(query.root(), query, dialect, names, false, Optional.empty(), stratificationTable);
		if (!compiled.step().isNegate()) {
			return compiled;
		}
		QueryStep root = QueryStepComposer.antiJoinWithAllEntities(
				compiled.step(), query.entitySchema(), rootNegationAction(query.root()), dialect);
		return new TranslatedQuerySteps(root, compiled.externalValues());
	}

	private static DateAggregationAction rootNegationAction(QueryNode node) {
		return switch (node) {
			case NegationNode negation -> negation.dateAction();
			case DateRestrictionNode restriction -> rootNegationAction(restriction.child());
			default -> DateAggregationAction.BLOCK;
		};
	}

	private static TranslatedQuerySteps translateNode(
			QueryNode node,
			ResolvedQuery query,
			TranslatorDialect dialect,
			SqlNameGenerator names,
			boolean negate,
			Optional<DateRange> restriction,
			Optional<QueryStep> stratificationTable
	) {
		return switch (node) {
			case AllEntitiesNode all -> new TranslatedQuerySteps(
					AllEntitiesQueryStepTranslator.translate(all, query.entitySchema(), dialect).toBuilder().negate(negate).build(), Optional.empty());
			case ExternalNode external -> {
				ExternalQuerySteps steps = ExternalQueryStepTranslator.translate(external, negate, restriction, dialect);
				yield new TranslatedQuerySteps(steps.entities(), steps.values());
			}
			case DateRestrictionNode restricted -> translateNode(
					restricted.child(), query, dialect, names, negate, Optional.of(restricted.dateRange()), stratificationTable);
			case NegationNode negation -> translateNode(negation.child(), query, dialect, names, true, restriction, stratificationTable);
			case AndNode and -> translateLogical(and.children(), JoinMode.INNER, and.dateAction(), and.createExists(),
					query, dialect, names, negate, restriction, stratificationTable);
			case OrNode or -> translateLogical(or.children(), JoinMode.FULL_OUTER, or.dateAction(), or.createExists(),
					query, dialect, names, negate, restriction, stratificationTable);
			case ConceptNode concept -> new TranslatedQuerySteps(
					ResolvedConceptTranslator.translate(concept, query.entitySchema(), dialect, names, negate, restriction, stratificationTable),
					Optional.empty());
		};
	}

	private static TranslatedQuerySteps translateLogical(
			List<QueryNode> children,
			JoinMode mode,
			DateAggregationAction action,
			boolean createExists,
			ResolvedQuery query,
			TranslatorDialect dialect,
			SqlNameGenerator names,
			boolean negate,
			Optional<DateRange> restriction,
			Optional<QueryStep> stratificationTable
	) {
		List<TranslatedQuerySteps> compiled = children.stream()
				.map(child -> translateNode(child, query, dialect, names, negate, restriction, stratificationTable)).toList();
		QueryStep step = LogicalQueryStepTranslator.translate(compiled.stream().map(TranslatedQuerySteps::step).toList(),
				mode, action, createExists, query.entitySchema(), dialect, names);
		Optional<QueryStep> external = compiled.stream().flatMap(value -> value.externalValues().stream())
				.reduce((first, second) -> second);
		return new TranslatedQuerySteps(step, external);
	}

	public record TranslatedQuerySteps(QueryStep step, Optional<QueryStep> externalValues) {
	}
}
