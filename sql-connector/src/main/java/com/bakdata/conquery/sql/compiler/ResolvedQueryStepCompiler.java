package com.bakdata.conquery.sql.compiler;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.AllEntitiesQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.ExternalQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.ExternalQuerySteps;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.LogicalQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStepComposer;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
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

/** Compiles a resolved query graph to reusable query-step IR without rendering a final projection. */
public final class ResolvedQueryStepCompiler {

	public CompiledQuerySteps compile(
			ResolvedQuery query,
			CompilerDialect dialect,
			SqlNameGenerator names,
			Optional<QueryStep> stratificationTable
	) {
		CompiledQuerySteps compiled = compileNode(query.root(), query, dialect, names, false, Optional.empty(), stratificationTable);
		if (!compiled.step().isNegate()) {
			return compiled;
		}
		QueryStep root = QueryStepComposer.antiJoinWithAllEntities(
				compiled.step(), query.entitySchema(), rootNegationAction(query.root()), dialect);
		return new CompiledQuerySteps(root, compiled.externalValues());
	}

	private static DateAggregationAction rootNegationAction(QueryNode node) {
		return switch (node) {
			case NegationNode negation -> negation.dateAction();
			case DateRestrictionNode restriction -> rootNegationAction(restriction.child());
			default -> DateAggregationAction.BLOCK;
		};
	}

	private static CompiledQuerySteps compileNode(
			QueryNode node,
			ResolvedQuery query,
			CompilerDialect dialect,
			SqlNameGenerator names,
			boolean negate,
			Optional<DateRange> restriction,
			Optional<QueryStep> stratificationTable
	) {
		return switch (node) {
			case AllEntitiesNode all -> new CompiledQuerySteps(
					AllEntitiesQueryStepCompiler.compile(all, query.entitySchema(), dialect).toBuilder().negate(negate).build(), Optional.empty());
			case ExternalNode external -> {
				ExternalQuerySteps steps = ExternalQueryStepCompiler.compile(external, negate, restriction, dialect);
				yield new CompiledQuerySteps(steps.entities(), steps.values());
			}
			case DateRestrictionNode restricted -> compileNode(
					restricted.child(), query, dialect, names, negate, Optional.of(restricted.dateRange()), stratificationTable);
			case NegationNode negation -> compileNode(negation.child(), query, dialect, names, true, restriction, stratificationTable);
			case AndNode and -> compileLogical(and.children(), JoinMode.INNER, and.dateAction(), and.createExists(),
					query, dialect, names, negate, restriction, stratificationTable);
			case OrNode or -> compileLogical(or.children(), JoinMode.FULL_OUTER, or.dateAction(), or.createExists(),
					query, dialect, names, negate, restriction, stratificationTable);
			case ConceptNode concept -> new CompiledQuerySteps(
					ResolvedConceptCompiler.compile(concept, query.entitySchema(), dialect, names, negate, restriction, stratificationTable),
					Optional.empty());
		};
	}

	private static CompiledQuerySteps compileLogical(
			List<QueryNode> children,
			JoinMode mode,
			DateAggregationAction action,
			boolean createExists,
			ResolvedQuery query,
			CompilerDialect dialect,
			SqlNameGenerator names,
			boolean negate,
			Optional<DateRange> restriction,
			Optional<QueryStep> stratificationTable
	) {
		List<CompiledQuerySteps> compiled = children.stream()
				.map(child -> compileNode(child, query, dialect, names, negate, restriction, stratificationTable)).toList();
		QueryStep step = LogicalQueryStepCompiler.compile(compiled.stream().map(CompiledQuerySteps::step).toList(),
				mode, action, createExists, query.entitySchema(), dialect, names);
		Optional<QueryStep> external = compiled.stream().flatMap(value -> value.externalValues().stream())
				.reduce((first, second) -> second);
		return new CompiledQuerySteps(step, external);
	}

	public record CompiledQuerySteps(QueryStep step, Optional<QueryStep> externalValues) {
	}
}
