package com.bakdata.conquery.sql.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.AllEntitiesQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.ExternalQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.ExternalQuerySteps;
import com.bakdata.conquery.sql.compiler.ir.FinalQueryStepComposer;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.LogicalQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStepComposer;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.compiler.rendering.QueryStepRenderer;
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
import com.bakdata.conquery.sql.model.result.ResultColumn;
import com.bakdata.conquery.sql.model.result.ResultType;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.conf.ParamType;

/** Default compiler for framework-neutral, fully resolved concept queries. */
public final class DefaultSqlCompiler implements SqlCompiler {

	private final QueryStepRenderer renderer;

	public DefaultSqlCompiler(DSLContext dslContext) {
		this.renderer = new QueryStepRenderer(dslContext);
	}

	@Override
	public CompiledQuery compile(ResolvedQuery query, CompilerDialect dialect) {
		SqlNameGenerator names = new SqlNameGenerator(dialect.getNameMaxLength());
		CompiledStep compiled = compileNode(query.root(), query.entitySchema(), dialect, names, false, Optional.empty());
		QueryStep root = compiled.step();
		if (root.isNegate()) {
			root = QueryStepComposer.antiJoinWithAllEntities(
					root, query.entitySchema(), rootNegationAction(query.root()), dialect);
		}
		QueryStep finalStep = FinalQueryStepComposer.compose(root, compiled.externalValues(), query.includeValidityDate(), dialect);
		Select<Record> rendered = renderer.toSelectQuery(finalStep, dialect);
		return new CompiledQuery(rendered.getSQL(ParamType.INLINED), compiledColumns(query, rendered.getSelect(), finalStep));
	}

	private static DateAggregationAction rootNegationAction(QueryNode node) {
		return switch (node) {
			case NegationNode negation -> negation.dateAction();
			case DateRestrictionNode restriction -> rootNegationAction(restriction.child());
			default -> DateAggregationAction.BLOCK;
		};
	}

	private static List<CompiledColumn> compiledColumns(ResolvedQuery query, List<? extends Field<?>> fields, QueryStep step) {
		int idCount = 1;
		if (fields.size() != idCount + query.resultColumns().size()) {
			throw new IllegalStateException("Compiled projection does not match the declared result columns");
		}
		List<CompiledColumn> columns = new ArrayList<>(fields.size());
		for (int index = 0; index < idCount; index++) {
			String outputId = index == 0 ? query.entitySchema().primaryId().logicalId() : "entity-id-%d".formatted(index + 1);
			columns.add(new CompiledColumn(outputId, fields.get(index).getName(), ResultType.Primitive.STRING, ColumnRole.ENTITY_ID));
		}
		for (int index = 0; index < query.resultColumns().size(); index++) {
			ResultColumn result = query.resultColumns().get(index);
			columns.add(new CompiledColumn(result.outputId(), fields.get(idCount + index).getName(), result.type(), ColumnRole.RESULT));
		}
		return columns;
	}

	private static CompiledStep compileNode(QueryNode node, EntitySchema schema, CompilerDialect dialect,
			SqlNameGenerator names, boolean negate, Optional<DateRange> restriction) {
		return switch (node) {
			case AllEntitiesNode all -> new CompiledStep(
					AllEntitiesQueryStepCompiler.compile(all, schema, dialect).toBuilder().negate(negate).build(), Optional.empty());
			case ExternalNode external -> {
				ExternalQuerySteps steps = ExternalQueryStepCompiler.compile(external, negate, restriction, dialect);
				yield new CompiledStep(steps.entities(), steps.values());
			}
			case DateRestrictionNode restricted -> compileNode(
					restricted.child(), schema, dialect, names, negate, Optional.of(restricted.dateRange()));
			case NegationNode negation -> compileNode(negation.child(), schema, dialect, names, true, restriction);
			case AndNode and -> compileLogical(and.children(), JoinMode.INNER, and.dateAction(), and.createExists(),
					schema, dialect, names, negate, restriction);
			case OrNode or -> compileLogical(or.children(), JoinMode.FULL_OUTER, or.dateAction(), or.createExists(),
					schema, dialect, names, negate, restriction);
			case ConceptNode concept -> new CompiledStep(
					ResolvedConceptCompiler.compile(concept, schema, dialect, names, negate, restriction), Optional.empty());
		};
	}

	private static CompiledStep compileLogical(List<QueryNode> children, JoinMode mode, DateAggregationAction action,
			boolean createExists, EntitySchema schema, CompilerDialect dialect, SqlNameGenerator names,
			boolean negate, Optional<DateRange> restriction) {
		List<CompiledStep> compiled = children.stream()
				.map(child -> compileNode(child, schema, dialect, names, negate, restriction)).toList();
		QueryStep step = LogicalQueryStepCompiler.compile(compiled.stream().map(CompiledStep::step).toList(),
				mode, action, createExists, schema, dialect, names);
		Optional<QueryStep> external = compiled.stream().flatMap(value -> value.externalValues().stream())
				.reduce((first, second) -> second);
		return new CompiledStep(step, external);
	}

	private record CompiledStep(QueryStep step, Optional<QueryStep> externalValues) {
	}
}
